package com.gec.anan.system.service;


import com.gec.anan.model.entity.system.SysUser;
import com.gec.anan.model.query.system.SysUserQuery;
import com.gec.anan.model.vo.base.PageVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface SysUserService extends IService<SysUser> {

    PageVo<SysUser> findPage(Page<SysUser> pageParam, SysUserQuery adminQuery);

    void updateStatus(Long id, Integer status);

    SysUser getByUsername(String username);

    /**
     * 根据用户名获取用户登录信息
     * @param userId
     * @return
     */
    Map<String, Object> getUserInfo(Long userId);
}
