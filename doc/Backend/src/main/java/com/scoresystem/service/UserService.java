package com.scoresystem.service;

import com.scoresystem.dto.ScoreSystemModels.UserDTO;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @return 用户DTO，包含token
     */
    UserDTO login(String username, String password);
    
    /**
     * 获取所有用户
     * 
     * @return 用户DTO列表
     */
    List<UserDTO> getAllUsers();
    
    /**
     * 保存用户（创建或更新）
     * 
     * @param userDTO 用户DTO
     * @return 保存后的用户DTO
     */
    UserDTO saveUser(UserDTO userDTO);
    
    /**
     * 删除用户
     * 
     * @param username 用户名
     */
    void deleteUser(String username);
    
    /**
     * 根据用户名获取用户
     * 
     * @param username 用户名
     * @return 用户DTO
     */
    UserDTO getUserByUsername(String username);
} 