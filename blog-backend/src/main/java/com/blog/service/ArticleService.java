package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.ArticleSaveRequest;
import com.blog.vo.ArticleAdminDetailVO;
import com.blog.vo.ArticleAdminVO;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListVO;
import com.blog.vo.ArchiveVO;

import java.util.List;

/**
 * 文章业务接口
 *
 * <p>包含两套读接口：
 * <ul>
 *   <li>前台：只返回 status=1（已发布）的文章，走多级缓存；</li>
 *   <li>后台：返回全部状态的文章，实时查询（保证管理端看到最新数据）。</li>
 * </ul>
 */
public interface ArticleService {

    /**
     * 前台文章分页（带多级缓存）
     *
     * @param page       页码
     * @param size       每页条数
     * @param categoryId 分类筛选（可选）
     * @param tagId      标签筛选（可选）
     * @param keyword    标题 / 摘要 / 正文关键字（可选）
     * @return 分页结果
     */
    PageResult<ArticleListVO> pageArticles(long page, long size, Long categoryId, Long tagId, String keyword);

    /**
     * 前台文章详情（缓存 + 浏览量 +1 + 上一篇 / 下一篇）
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    ArticleDetailVO getArticleDetail(Long id);

    /**
     * 前台归档：按年份倒序返回已发布文章
     *
     * @return 归档分组列表
     */
    List<ArchiveVO> listArchive();

    /**
     * 后台文章分页
     *
     * @param page    页码
     * @param size    每页条数
     * @param keyword 标题 / 摘要关键字（可选）
     * @param status  状态筛选 0/1（可选）
     * @return 分页结果
     */
    PageResult<ArticleAdminVO> pageAdminArticles(long page, long size, String keyword, Integer status);

    /**
     * 后台文章详情（包含正文与标签 ID，供编辑页回填）
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    ArticleAdminDetailVO getAdminArticle(Long id);

    /**
     * 新增文章，同时维护文章-标签关联
     *
     * @param request 文章内容
     */
    void createArticle(ArticleSaveRequest request);

    /**
     * 修改文章，同时整体覆盖文章-标签关联
     *
     * @param id      文章 ID
     * @param request 文章内容
     */
    void updateArticle(Long id, ArticleSaveRequest request);

    /**
     * 删除文章（逻辑删除 + 清理标签关联 + 清理浏览量计数）
     *
     * @param id 文章 ID
     */
    void deleteArticle(Long id);

    /**
     * 置顶 / 取消置顶
     *
     * @param id    文章 ID
     * @param isTop true=置顶，false=取消
     */
    void updateTop(Long id, Boolean isTop);
}
