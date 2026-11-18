package com.blog.service;

import com.blog.dto.TagSaveRequest;
import com.blog.vo.TagVO;

import java.util.List;

/**
 * 标签业务接口
 */
public interface TagService {

    /** 标签列表（带缓存的读接口，含文章数量） */
    List<TagVO> listTags();

    /** 新增标签 */
    void createTag(TagSaveRequest request);

    /** 修改标签 */
    void updateTag(Long id, TagSaveRequest request);

    /** 删除标签（同时清理文章-标签关联） */
    void deleteTag(Long id);
}
