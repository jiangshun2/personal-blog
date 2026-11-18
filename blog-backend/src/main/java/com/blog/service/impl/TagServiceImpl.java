package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.cache.CacheKeys;
import com.blog.cache.CacheService;
import com.blog.common.ResultCode;
import com.blog.common.exception.BusinessException;
import com.blog.dto.IdCountDTO;
import com.blog.dto.TagSaveRequest;
import com.blog.entity.ArticleTag;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import com.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标签业务实现
 *
 * <p>缓存策略：标签列表缓存键 blog:tag:list，TTL 30 分钟。
 * 标签改名会影响文章列表 / 详情中展示的标签名，因此改名 / 删除后
 * 除了失效标签列表，还要按前缀清理文章相关缓存。
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleMapper articleMapper;
    private final CacheService cacheService;

    @Override
    public List<TagVO> listTags() {
        // 读穿多级缓存：Caffeine → Redis → MySQL
        return cacheService.getOrLoad(
                CacheKeys.TAG_LIST,
                CacheKeys.TAG_LIST_TTL_SECONDS,
                this::loadTags);
    }

    @Override
    public void createTag(TagSaveRequest request) {
        checkNameDuplicate(request.getName(), null);

        Tag tag = new Tag();
        tag.setName(request.getName().trim());
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "标签名称已存在");
        }
        cacheService.evict(CacheKeys.TAG_LIST);
    }

    @Override
    public void updateTag(Long id, TagSaveRequest request) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "标签不存在");
        }
        checkNameDuplicate(request.getName(), id);

        Tag update = new Tag();
        update.setId(id);
        update.setName(request.getName().trim());
        try {
            tagMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "标签名称已存在");
        }

        // 标签名展示在文章列表 / 详情中，改名后清理文章缓存
        cacheService.evict(CacheKeys.TAG_LIST);
        cacheService.evictByPrefix(CacheKeys.ARTICLE_DETAIL);
    }

    @Override
    public void deleteTag(Long id) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "标签不存在");
        }
        // 逻辑删除标签
        tagMapper.deleteById(id);
        // 物理删除文章-标签关联，避免文章详情查出已删除的标签
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, id));

        // 标签下文章数量变化 + 文章标签展示变化，清理相关缓存
        cacheService.evict(CacheKeys.TAG_LIST);
        cacheService.evictByPrefix(CacheKeys.ARTICLE_DETAIL);
    }

    /** 标签列表回源：按 id 升序，并统计每个标签下已发布文章数量 */
    private List<TagVO> loadTags() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getId));
        if (tags.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Long> countMap = articleMapper.countArticleByTag().stream()
                .collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCountValue));

        List<TagVO> result = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            TagVO vo = new TagVO();
            vo.setId(tag.getId());
            vo.setName(tag.getName());
            vo.setArticleCount(countMap.getOrDefault(tag.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    /** 校验标签名称是否重复（修改时排除自身） */
    private void checkNameDuplicate(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "标签名称不能为空");
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(Tag::getId, excludeId);
        }
        Long count = tagMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "标签名称已存在");
        }
    }
}
