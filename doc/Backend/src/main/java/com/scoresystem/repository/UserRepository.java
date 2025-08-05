package com.scoresystem.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scoresystem.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 用户数据访问接口
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
    
    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据角色查找用户列表
     * 
     * @param role 角色
     * @return 用户实体列表
     */
    @Select("SELECT * FROM users WHERE role = #{role}")
    List<User> findByRole(@Param("role") String role);
    
    /**
     * 根据部门查找用户列表
     * 
     * @param department 部门
     * @return 用户实体列表
     */
    @Select("SELECT * FROM users WHERE department = #{department}")
    List<User> findByDepartment(@Param("department") String department);
    
    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) FROM users WHERE username = #{username}")
    int countByUsername(@Param("username") String username);
    
    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    default boolean existsByUsername(String username) {
        return countByUsername(username) > 0;
    }
    
    /**
     * 统计专家用户数量
     * 
     * @return 专家用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE role = 'EXPERT'")
    int countExperts();
    
    /**
     * 统计指定时间段内创建的用户数量
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE create_time BETWEEN #{startDate} AND #{endDate}")
    int countByCreateTimeBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 根据用户名列表查找用户列表
     * 
     * @param usernames 用户名列表
     * @return 用户实体列表
     */
    @Select({
        "<script>",
        "SELECT * FROM users WHERE username IN",
        "<foreach collection='usernames' item='item' open='(' separator=',' close=')'>",
        "#{item}",
        "</foreach>",
        "</script>"
    })
    List<User> findByUsernames(@Param("usernames") List<String> usernames);
}