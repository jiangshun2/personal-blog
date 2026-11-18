package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.cache.CacheKeys;
import com.blog.cache.CacheService;
import com.blog.cache.ViewCountService;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.common.exception.BusinessException;
import com.blog.dto.ArticleSaveRequest;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.ArticleService;
import com.blog.vo.ArticleAdminDetailVO;
import com.blog.vo.ArticleAdminVO;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListVO;
import com.blog.vo.ArchiveItemVO;
import com.blog.vo.ArchiveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章业务实现
 *
 * <p>本类是 M2 业务逻辑 + M5 缓存 / 浏览量能力的结合点，读路径与缓存的关系：
 * <ul>
 *   <li>前台分页：CacheService 多级缓存（键 blog:article:page:...，TTL 5 分钟）；</li>
 *   <li>前台详情：CacheService 多级缓存（键 blog:article:{id}，TTL 30 分钟）+ Redis 浏览量 INCR；</li>
 *   <li>写操作：更新数据库后精准失效详情键、按前缀清理分页列表缓存。</li>
 * </ul>
 * 注意：缓存中保存的 viewCount 是「数据库值」，返回给前端时会再加上 Redis 增量，
 * 所以从缓存取出后必须复制 VO 再改浏览量，不能直接修改缓存对象。
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CacheService cacheService;
    private final ViewCountService viewCountService;

    // ============================ 前台读接口 ============================

    @Override
    public PageResult<ArticleListVO> pageArticles(long page, long size, Long categoryId, Long tagId, String keyword) {
        // 1. 组装缓存键（契约：blog:article:page:{page}:{size}:{categoryId}:{tagId}:{keyword}）
        String cacheKey = CacheKeys.articlePage(page, size, categoryId, tagId, keyword);

        // 2. 走多级缓存（Caffeine → Redis → MySQL），未命中时回源加载并回填两级
        PageResult<ArticleListVO> cached = cacheService.getOrLoad(
                cacheKey,
                CacheKeys.ARTICLE_PAGE_TTL_SECONDS,
                () -> loadArticlePage(page, size, categoryId, tagId, keyword));

        // 3. 缓存里的 viewCount 是数据库值，叠加 Redis 增量后再返回（复制对象，避免污染缓存）
        return withViewIncrement(cached);
    }

    @Override
    public ArticleDetailVO getArticleDetail(Long id) {
        // 1. 走多级缓存加载详情（缓存中是数据库原始浏览量）
        ArticleDetailVO cached = cacheService.getOrLoad(
                CacheKeys.articleDetail(id),
                CacheKeys.ARTICLE_DETAIL_TTL_SECONDS,
                () -> loadArticleDetailFromDb(id));

        // 2. 每次访问浏览量 +1（Redis INCR，不直接写库）
        long increment = viewCountService.increment(id);

        // 3. 复制 VO 后叠加增量返回，保证「数据库值 + Redis 增量」
        ArticleDetailVO result = new ArticleDetailVO();
        BeanUtils.copyProperties(cached, result);
        result.setViewCount(safeViewCount(cached.getViewCount()) + increment);
        return result;
    }

    @Override
    public List<ArchiveVO> listArchive() {
        // 归档不缓存（数据量小、更新频率低但访问也不高），直接查已发布文章的 id / 标题 / 时间
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getTitle, Article::getCreateTime)
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getCreateTime)
                .orderByDesc(Article::getId));

        // 按年份分组（查询结果已按时间倒序，年份天然从新到旧）
        Map<Integer, List<ArchiveItemVO>> grouped = new LinkedHashMap<>();
        for (Article article : articles) {
            if (article.getCreateTime() == null) {
                continue;
            }
            ArchiveItemVO item = new ArchiveItemVO();
            item.setId(article.getId());
            item.setTitle(article.getTitle());
            item.setCreateTime(article.getCreateTime());
            grouped.computeIfAbsent(article.getCreateTime().getYear(), year -> new ArrayList<>()).add(item);
        }

        // 显式按年份倒序输出，避免依赖查询顺序
        List<ArchiveVO> result = new ArrayList<>(grouped.size());
        grouped.keySet().stream()
                .sorted(Collections.reverseOrder())
                .forEach(year -> {
                    ArchiveVO archiveVO = new ArchiveVO();
                    archiveVO.setYear(year);
                    archiveVO.setArticles(grouped.get(year));
                    result.add(archiveVO);
                });
        return result;
    }

    // ============================ 后台读接口 ============================

    @Override
    public PageResult<ArticleAdminVO> pageAdminArticles(long page, long size, String keyword, Integer status) {
        // 后台列表不缓存：管理端需要实时看到最新数据
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Article::getTitle, kw).or().like(Article::getSummary, kw));
        }
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }
        wrapper.orderByDesc(Article::getCreateTime).orderByDesc(Article::getId);

        Page<Article> mpPage = articleMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(toArticleAdminVO(mpPage.getRecords()),
                mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    @Override
    public ArticleAdminDetailVO getAdminArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }
        // 先复用列表项转换（包含分类名、标签、浏览量叠加增量），再补充正文与标签 ID
        ArticleAdminVO base = toArticleAdminVO(List.of(article)).get(0);
        ArticleAdminDetailVO vo = new ArticleAdminDetailVO();
        BeanUtils.copyProperties(base, vo);
        vo.setContent(article.getContent());
        vo.setTagIds(loadTagIdsByArticleId(id));
        return vo;
    }

    // ============================ 后台写接口 ============================

    @Override
    public void createArticle(ArticleSaveRequest request) {
        // 作者取当前登录用户（JWT 过滤器解析后放入 SecurityContext）
        Long authorId = getCurrentUserId();
        validateCategory(request.getCategoryId());

        Article article = new Article();
        applyRequestFields(article, request, 0L);
        article.setAuthorId(authorId);
        articleMapper.insert(article);

        // 维护文章-标签关联
        saveArticleTags(article.getId(), request.getTagIds());

        // 精准失效：文章列表分页缓存全部按前缀清理（新文章可能出现在任意一页）
        cacheService.evictByPrefix(CacheKeys.ARTICLE_PAGE_PREFIX);
    }

    @Override
    public void updateArticle(Long id, ArticleSaveRequest request) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }
        validateCategory(request.getCategoryId());

        // 用 LambdaUpdateWrapper 显式 set：这样把 summary / content / categoryId 改成 null 时也能写入，
        // 而 updateById(entity) 默认会忽略 null 字段。
        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, id)
                .set(Article::getTitle, request.getTitle())
                .set(Article::getSummary, request.getSummary())
                .set(Article::getContent, request.getContent())
                .set(Article::getCover, request.getCover())
                .set(Article::getCategoryId, request.getCategoryId())
                .set(Article::getStatus, request.getStatus() == null ? 1 : request.getStatus())
                .set(Article::getIsTop, Boolean.TRUE.equals(request.getIsTop()) ? 1 : 0)
                // wrapper 方式不会触发自动填充，这里手动更新时间
                .set(Article::getUpdateTime, LocalDateTime.now());
        articleMapper.update(null, updateWrapper);

        // 标签关联：先删后插，实现整体覆盖
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        saveArticleTags(id, request.getTagIds());

        // 精准失效：详情按 id 删除，列表按前缀删除
        cacheService.evict(CacheKeys.articleDetail(id));
        cacheService.evictByPrefix(CacheKeys.ARTICLE_PAGE_PREFIX);
    }

    @Override
    public void deleteArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }
        // 逻辑删除文章
        articleMapper.deleteById(id);
        // 物理删除文章-标签关联（关联表没有逻辑删除字段）
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        // 清理浏览量计数键，避免定时任务继续回写已删除文章
        viewCountService.clear(id);

        cacheService.evict(CacheKeys.articleDetail(id));
        cacheService.evictByPrefix(CacheKeys.ARTICLE_PAGE_PREFIX);
    }

    @Override
    public void updateTop(Long id, Boolean isTop) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }
        Article update = new Article();
        update.setId(id);
        update.setIsTop(Boolean.TRUE.equals(isTop) ? 1 : 0);
        // 只需要更新 isTop，updateTime 由 MyMetaObjectHandler 自动填充
        articleMapper.updateById(update);

        // 置顶会影响列表顺序，必须清理分页缓存
        cacheService.evictByPrefix(CacheKeys.ARTICLE_PAGE_PREFIX);
    }

    // ============================ 前台分页 / 详情回源逻辑 ============================

    /**
     * 前台分页回源：查询已发布文章并按缓存需要组装 VO（viewCount 为数据库值）
     */
    private PageResult<ArticleListVO> loadArticlePage(long page, long size,
                                                      Long categoryId, Long tagId, String keyword) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        // 前台只展示已发布文章
        wrapper.eq(Article::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }
        if (tagId != null) {
            // 标签筛选：先查关联表拿到文章 ID 集合，再用 IN 过滤
            List<Long> articleIds = findArticleIdsByTag(tagId);
            if (articleIds.isEmpty()) {
                // 该标签下没有文章，直接返回空页（也会被缓存，避免反复查库）
                return new PageResult<>(new ArrayList<>(), 0L, page, size);
            }
            wrapper.in(Article::getId, articleIds);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            // 标题、摘要、正文任一命中即可
            wrapper.and(w -> w.like(Article::getTitle, kw)
                    .or().like(Article::getSummary, kw)
                    .or().like(Article::getContent, kw));
        }
        // 置顶优先，其次按创建时间倒序
        wrapper.orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreateTime)
                .orderByDesc(Article::getId);

        Page<Article> mpPage = articleMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(toArticleListVO(mpPage.getRecords()),
                mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }

    /**
     * 前台详情回源：查文章 + 分类名 + 标签 + 上一篇 / 下一篇
     *
     * <p>返回的 viewCount 是数据库值（Redis 增量在 getArticleDetail 中叠加）。
     */
    private ArticleDetailVO loadArticleDetailFromDb(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            // 未发布 / 已删除的文章对前台不可见，统一返回「资源不存在」
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在或已下架");
        }

        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setCover(article.getCover());
        vo.setCategoryId(article.getCategoryId());
        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            vo.setCategoryName(category == null ? null : category.getName());
        }
        vo.setTags(loadTagNamesByArticleId(id));
        vo.setViewCount(safeViewCount(article.getViewCount()));
        vo.setCreateTime(article.getCreateTime());

        // 上一篇：ID 更小的最近一篇已发布文章（发布时间更早）
        Article prev = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .lt(Article::getId, id)
                .orderByDesc(Article::getId)
                .last("LIMIT 1"));
        if (prev != null) {
            vo.setPrevId(prev.getId());
            vo.setPrevTitle(prev.getTitle());
        }

        // 下一篇：ID 更大的最近一篇已发布文章（发布时间更晚）
        Article next = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .gt(Article::getId, id)
                .orderByAsc(Article::getId)
                .last("LIMIT 1"));
        if (next != null) {
            vo.setNextId(next.getId());
            vo.setNextTitle(next.getTitle());
        }
        return vo;
    }

    // ============================ 实体 / VO 转换 ============================

    /** 文章实体列表 → 前台列表 VO（一次批量加载分类名与标签，避免 N+1 查询） */
    private List<ArticleListVO> toArticleListVO(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> categoryNames = loadCategoryNames(articles);
        Map<Long, List<Tag>> tagsByArticle = loadTagsByArticleIds(
                articles.stream().map(Article::getId).toList());

        List<ArticleListVO> result = new ArrayList<>(articles.size());
        for (Article article : articles) {
            ArticleListVO vo = new ArticleListVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCover(article.getCover());
            vo.setCategoryId(article.getCategoryId());
            vo.setCategoryName(categoryNames.get(article.getCategoryId()));
            vo.setTags(tagNames(tagsByArticle.get(article.getId())));
            vo.setViewCount(safeViewCount(article.getViewCount()));
            vo.setIsTop(Integer.valueOf(1).equals(article.getIsTop()));
            vo.setCreateTime(article.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    /** 文章实体列表 → 后台列表 VO（浏览量叠加 Redis 增量，管理端看到实时值） */
    private List<ArticleAdminVO> toArticleAdminVO(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> categoryNames = loadCategoryNames(articles);
        Map<Long, List<Tag>> tagsByArticle = loadTagsByArticleIds(
                articles.stream().map(Article::getId).toList());

        List<ArticleAdminVO> result = new ArrayList<>(articles.size());
        for (Article article : articles) {
            ArticleAdminVO vo = new ArticleAdminVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCover(article.getCover());
            vo.setCategoryId(article.getCategoryId());
            vo.setCategoryName(categoryNames.get(article.getCategoryId()));
            vo.setTags(tagNames(tagsByArticle.get(article.getId())));
            vo.setStatus(article.getStatus());
            vo.setViewCount(safeViewCount(article.getViewCount())
                    + viewCountService.getIncrement(article.getId()));
            vo.setIsTop(Integer.valueOf(1).equals(article.getIsTop()));
            vo.setCreateTime(article.getCreateTime());
            vo.setUpdateTime(article.getUpdateTime());
            result.add(vo);
        }
        return result;
    }

    /** 批量查询分类 ID → 分类名 */
    private Map<Long, String> loadCategoryNames(Collection<Article> articles) {
        Set<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    /** 批量查询「文章 ID → 标签列表」：先查关联表，再批量查标签 */
    private Map<Long, List<Tag>> loadTagsByArticleIds(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        List<ArticleTag> relations = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIds)
                .orderByAsc(ArticleTag::getId));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Set<Long> tagIds = relations.stream().map(ArticleTag::getTagId).collect(Collectors.toSet());
        // selectBatchIds 会自动过滤逻辑删除的标签
        Map<Long, Tag> tagById = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, tag -> tag));

        Map<Long, List<Tag>> result = new HashMap<>();
        for (ArticleTag relation : relations) {
            Tag tag = tagById.get(relation.getTagId());
            if (tag != null) {
                result.computeIfAbsent(relation.getArticleId(), key -> new ArrayList<>()).add(tag);
            }
        }
        return result;
    }

    /** 查询某篇文章的标签名称列表 */
    private List<String> loadTagNamesByArticleId(Long articleId) {
        return tagNames(loadTagsByArticleIds(List.of(articleId)).get(articleId));
    }

    /** 查询某篇文章的标签 ID 列表（后台编辑页回填用） */
    private List<Long> loadTagIdsByArticleId(Long articleId) {
        return articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                        .eq(ArticleTag::getArticleId, articleId)
                        .orderByAsc(ArticleTag::getId))
                .stream()
                .map(ArticleTag::getTagId)
                .toList();
    }

    /** 标签实体列表 → 标签名列表（为空时返回空列表，保证 JSON 中是 [] 而不是 null） */
    private List<String> tagNames(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        return tags.stream().map(Tag::getName).toList();
    }

    /** 根据标签查文章 ID 列表 */
    private List<Long> findArticleIdsByTag(Long tagId) {
        return articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                        .select(ArticleTag::getArticleId)
                        .eq(ArticleTag::getTagId, tagId))
                .stream()
                .map(ArticleTag::getArticleId)
                .distinct()
                .toList();
    }

    /**
     * 给前台分页结果的每条记录叠加 Redis 浏览量增量
     *
     * <p>必须复制 VO：缓存对象（尤其是 Caffeine 中的对象）在多个请求间共享，
     * 直接修改会把增量重复累加。
     */
    private PageResult<ArticleListVO> withViewIncrement(PageResult<ArticleListVO> pageResult) {
        List<ArticleListVO> records = pageResult.getRecords();
        List<ArticleListVO> resultRecords = new ArrayList<>(records.size());
        for (ArticleListVO item : records) {
            ArticleListVO vo = new ArticleListVO();
            BeanUtils.copyProperties(item, vo);
            vo.setViewCount(safeViewCount(item.getViewCount())
                    + viewCountService.getIncrement(item.getId()));
            resultRecords.add(vo);
        }
        return new PageResult<>(resultRecords, pageResult.getTotal(),
                pageResult.getCurrent(), pageResult.getSize());
    }

    // ============================ 写操作辅助方法 ============================

    /** 把请求字段写入实体（新建文章时调用） */
    private void applyRequestFields(Article article, ArticleSaveRequest request, Long viewCount) {
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCover(request.getCover());
        article.setCategoryId(request.getCategoryId());
        article.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        article.setIsTop(Boolean.TRUE.equals(request.getIsTop()) ? 1 : 0);
        article.setViewCount(viewCount);
    }

    /** 校验分类是否存在（分类为可选字段，为空表示未分类） */
    private void validateCategory(Long categoryId) {
        if (categoryId != null && categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类不存在");
        }
    }

    /**
     * 保存文章-标签关联（只保存真实存在的标签，避免脏 ID）
     *
     * <p>关联表没有逻辑删除，调用前先物理删除旧关联即可实现「整体覆盖」。
     */
    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> distinctTagIds = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctTagIds.isEmpty()) {
            return;
        }
        List<Tag> existingTags = tagMapper.selectBatchIds(distinctTagIds);
        for (Tag tag : existingTags) {
            ArticleTag relation = new ArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tag.getId());
            articleTagMapper.insert(relation);
        }
    }

    /** 获取当前登录用户 ID（后台接口已由 Spring Security 保证已登录） */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /** 浏览量兜底：数据库字段可能为 null */
    private long safeViewCount(Long viewCount) {
        return viewCount == null ? 0L : viewCount;
    }
}
