package com.scoresystem.service.impl;

import com.scoresystem.dto.ScoreSystemModels.UserDTO;
import com.scoresystem.model.User;
import com.scoresystem.repository.UserRepository;
import com.scoresystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService测试类
 * 
 * 测试说明：
 * 1. 使用@SpringBootTest进行集成测试
 * 2. 使用@ActiveProfiles("sqlserver")指定使用SQL Server数据库
 * 3. 使用@Transactional确保测试数据回滚，不影响数据库
 * 4. 测试包括：用户登录、获取所有用户、保存用户、删除用户、根据用户名获取用户等功能
 */
@SpringBootTest
@ActiveProfiles("sqlserver")
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    private UserDTO testUserDTO;
    
    /**
     * 测试前准备
     * 创建测试用户数据
     */
    @BeforeEach
    public void setUp() {
        // 清理可能存在的测试用户
        try {
            userRepository.deleteById("testuser");
        } catch (Exception e) {
            // 忽略异常
        }
        
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setName("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setRole("user");
        testUser.setDepartment("测试部门");
        testUser.setCreateTime(new Date());
        testUser.setUpdateTime(new Date());
        
        userRepository.insert(testUser);
        
        // 创建测试UserDTO
        testUserDTO = new UserDTO();
        testUserDTO.setUsername("newtestuser");
        testUserDTO.setPassword("newpassword123");
        testUserDTO.setName("新测试用户");
        testUserDTO.setEmail("newtest@example.com");
        testUserDTO.setRole("user");
        testUserDTO.setDepartment("新测试部门");
    }
    
    /**
     * 测试用户登录成功
     * 验证用户使用正确的用户名和密码能够成功登录
     */
    @Test
    @DisplayName("测试用户登录成功")
    public void testLoginSuccess() {
        UserDTO result = userService.login("testuser", "password123");
        
        assertNotNull(result, "登录应该成功返回用户信息");
        assertEquals("testuser", result.getUsername(), "返回的用户名应该匹配");
        assertEquals("测试用户", result.getName(), "返回的用户名称应该匹配");
        assertNotNull(result.getToken(), "登录成功应返回token");
    }
    
    /**
     * 测试用户登录失败 - 错误的密码
     * 验证用户使用错误的密码无法登录
     */
    @Test
    @DisplayName("测试用户登录失败 - 错误的密码")
    public void testLoginFailureWrongPassword() {
        assertThrows(BadCredentialsException.class, () -> {
            userService.login("testuser", "wrongpassword");
        }, "使用错误的密码应该抛出BadCredentialsException异常");
    }
    
    /**
     * 测试用户登录失败 - 用户不存在
     * 验证不存在的用户无法登录
     */
    @Test
    @DisplayName("测试用户登录失败 - 用户不存在")
    public void testLoginFailureUserNotFound() {
        assertThrows(BadCredentialsException.class, () -> {
            userService.login("nonexistentuser", "password123");
        }, "使用不存在的用户名应该抛出BadCredentialsException异常");
    }
    
    /**
     * 测试获取所有用户
     * 验证能够正确获取所有用户列表
     */
    @Test
    @DisplayName("测试获取所有用户")
    public void testGetAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        
        assertNotNull(users, "用户列表不应为空");
        assertFalse(users.isEmpty(), "用户列表不应为空");
        
        // 验证测试用户在列表中
        boolean found = users.stream()
                .anyMatch(u -> "testuser".equals(u.getUsername()));
        assertTrue(found, "测试用户应该在用户列表中");
    }
    
    /**
     * 测试保存新用户
     * 验证能够正确创建新用户
     */
    @Test
    @DisplayName("测试保存新用户")
    public void testSaveNewUser() {
        UserDTO savedUser = userService.saveUser(testUserDTO);
        
        assertNotNull(savedUser, "保存的用户不应为空");
        assertEquals("newtestuser", savedUser.getUsername(), "用户名应该匹配");
        assertEquals("新测试用户", savedUser.getName(), "用户名称应该匹配");
        
        // 验证用户已保存到数据库
        User dbUser = userRepository.findByUsername("newtestuser");
        assertNotNull(dbUser, "用户应该已保存到数据库");
        assertEquals("新测试用户", dbUser.getName(), "数据库中的用户名称应该匹配");
    }
    
    /**
     * 测试更新现有用户
     * 验证能够正确更新现有用户
     */
    @Test
    @DisplayName("测试更新现有用户")
    public void testUpdateExistingUser() {
        // 修改测试用户信息
        testUserDTO.setUsername("testuser"); // 使用现有用户名
        testUserDTO.setName("更新的测试用户");
        testUserDTO.setEmail("updated@example.com");
        
        UserDTO updatedUser = userService.saveUser(testUserDTO);
        
        assertNotNull(updatedUser, "更新的用户不应为空");
        assertEquals("testuser", updatedUser.getUsername(), "用户名应该匹配");
        assertEquals("更新的测试用户", updatedUser.getName(), "更新后的用户名称应该匹配");
        assertEquals("updated@example.com", updatedUser.getEmail(), "更新后的邮箱应该匹配");
        
        // 验证用户已更新到数据库
        User dbUser = userRepository.findByUsername("testuser");
        assertNotNull(dbUser, "用户应该存在于数据库中");
        assertEquals("更新的测试用户", dbUser.getName(), "数据库中的用户名称应该已更新");
        assertEquals("updated@example.com", dbUser.getEmail(), "数据库中的邮箱应该已更新");
    }
    
    /**
     * 测试删除用户
     * 验证能够正确删除用户
     */
    @Test
    @DisplayName("测试删除用户")
    public void testDeleteUser() {
        userService.deleteUser("testuser");
        
        // 验证用户已从数据库中删除
        User dbUser = userRepository.findByUsername("testuser");
        assertNull(dbUser, "用户应该已从数据库中删除");
    }
    
    /**
     * 测试根据用户名获取用户
     * 验证能够正确获取指定用户名的用户
     */
    @Test
    @DisplayName("测试根据用户名获取用户")
    public void testGetUserByUsername() {
        UserDTO user = userService.getUserByUsername("testuser");
        
        assertNotNull(user, "应该能找到测试用户");
        assertEquals("testuser", user.getUsername(), "用户名应该匹配");
        assertEquals("测试用户", user.getName(), "用户名称应该匹配");
    }
    
    /**
     * 测试根据用户名获取不存在的用户
     * 验证获取不存在用户时返回null
     */
    @Test
    @DisplayName("测试根据用户名获取不存在的用户")
    public void testGetNonExistentUserByUsername() {
        UserDTO user = userService.getUserByUsername("nonexistentuser");
        
        assertNull(user, "不存在的用户应该返回null");
    }
}
