package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.cache.CacheKeys;
import com.blog.cache.CacheService;
import com.blog.common.ResultCode;
import com.blog.common.exception.BusinessException;
import com.blog.dto.CategorySaveRequest;
import com.blog.dto.IdCountDTO;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.service.CategoryService;
import com.blog.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类业务实现
 *
 * <p>缓存策略：分类列表缓存键 blog:category:list，TTL 30 分钟；
 * 分类新增 / 修改 / 删除后失效分类列表缓存。
 * 由于文章列表 / 详情里也会展示分类名，分类改名后还需按前缀清理文章相关缓存。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;
    private final CacheService cacheService;

    @Override
    public List<CategoryVO> listCategories() {
        // 读穿多级缓存：Caffeine → Redis → MySQL
        return cacheService.getOrLoad(
                CacheKeys.CATEGORY_LIST,
                CacheKeys.CATEGORY_LIST_TTL_SECONDS,
                this::loadCategories);
    }

    @Override
    public void createCategory(CategorySaveRequest request) {
        checkNameDuplicate(request.getName(), null);

        Category category = new Category();
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突（含逻辑删除的历史分类同名）统一转成业务异常
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类名称已存在");
        }

        // 写后精准失效分类列表缓存
        cacheService.evict(CacheKeys.CATEGORY_LIST);
    }

    @Override
    public void updateCategory(Long id, CategorySaveRequest request) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }
        checkNameDuplicate(request.getName(), id);

        // 显式 set，保证 description 也能被改成 null
        LambdaUpdateWrapper<Category> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Category::getId, id)
                .set(Category::getName, request.getName().trim())
                .set(Category::getDescription, request.getDescription())
                .set(Category::getSort, request.getSort() == null ? 0 : request.getSort())
                .set(Category::getUpdateTime, LocalDateTime.now());
        try {
            categoryMapper.update(null, updateWrapper);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类名称已存在");
        }

        // 分类名出现在文章列表 / 详情中，按前缀清理全部文章缓存
        cacheService.evict(CacheKeys.CATEGORY_LIST);
        cacheService.evictByPrefix(CacheKeys.ARTICLE_DETAIL);
    }

    @Override
    public void deleteCategory(Long id) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分类不存在");
        }
        // 分类下还有文章（含草稿）时禁止删除，避免文章分类悬空
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getCategoryId, id));
        if (articleCount != null && articleCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该分类下还有 " + articleCount + " 篇文章，无法删除");
        }
        categoryMapper.deleteById(id);
        cacheService.evict(CacheKeys.CATEGORY_LIST);
    }

    /** 分类列表回源：按 sort、id 升序，并统计每个分类下已发布文章数量 */
    private List<CategoryVO> loadCategories() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        if (categories.isEmpty()) {
            return new ArrayList<>();
        }
        // 一次聚合查询拿到所有分类的文章数，避免逐个分类 count
        Map<Long, Long> countMap = articleMapper.countArticleByCategory().stream()
                .collect(Collectors.toMap(IdCountDTO::getId, IdCountDTO::getCountValue));

        List<CategoryVO> result = new ArrayList<>(categories.size());
        for (Category category : categories) {
            CategoryVO vo = new CategoryVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setDescription(category.getDescription());
            vo.setSort(category.getSort());
            vo.setArticleCount(countMap.getOrDefault(category.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    /** 校验分类名称是否重复（修改时排除自身） */
    private void checkNameDuplicate(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类名称不能为空");
        }
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类名称已存在");
        }
    }
}
