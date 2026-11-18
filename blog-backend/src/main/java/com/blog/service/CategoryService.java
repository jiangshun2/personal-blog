package com.blog.service;

import com.blog.dto.CategorySaveRequest;
import com.blog.vo.CategoryVO;

import java.util.List;

/**
 * 分类业务接口
 */
public interface CategoryService {

    /** 分类列表（带缓存的读接口，含文章数量） */
    List<CategoryVO> listCategories();

    /** 新增分类 */
    void createCategory(CategorySaveRequest request);

    /** 修改分类 */
    void updateCategory(Long id, CategorySaveRequest request);

    /** 删除分类（分类下还有文章时禁止删除） */
    void deleteCategory(Long id);
}
