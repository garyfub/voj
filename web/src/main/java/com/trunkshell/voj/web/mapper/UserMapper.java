package com.trunkshell.voj.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.trunkshell.voj.web.model.Language;
import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.model.UserGroup;

/**
 * User Data Access Object.
 * 
 * @author Xie Haozhe
 */
@CacheNamespace(implementation = org.mybatis.caches.ehcache.EhcacheCache.class)
public interface UserMapper {
    /**
     * [此方法仅供管理员使用]
     * 获取系统中某个用户组中用户的总数.
     * @param userGroup - 用户所属的用户组对象
     * @return 系统中某个用户组中用户的总数
     */
    @Select("SELECT COUNT(*) FROM voj_users WHERE user_group_id = #{userGroup.userGroupId}")
    @Options(useCache = true)
    public long getNumberOfUsersUsingUserGroup(@Param("userGroup") UserGroup userGroup);
    
    /**
     * 通过用户唯一标识符获取用户对象.
     * @param uid - 用户唯一标识符
     * @return 预期的用户对象或空引用
     */
    @Select("SELECT * FROM voj_users WHERE uid = #{uid}")
    @Options(useCache = true)
    @Results(value = {
        @Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.web.mapper.UserGroupMapper.getUserGroupUsingId")),
        @Result(property = "preferLanguage", column = "prefer_language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.web.mapper.LanguageMapper.getLanguageUsingId"))
    })
    public User getUserUsingUid(@Param("uid") long uid);
    
    /**
     * 通过用户名获取用户对象.
     * @param username - 用户名
     * @return 预期的用户对象或空引用
     */
    @Select("SELECT * FROM voj_users WHERE username = #{username}")
    @Options(useCache = true)
    @Results(value = {
        @Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.web.mapper.UserGroupMapper.getUserGroupUsingId")),
        @Result(property = "preferLanguage", column = "prefer_language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.web.mapper.LanguageMapper.getLanguageUsingId"))
    })
    public User getUserUsingUsername(@Param("username") String username);
    
    /**
     * 通过电子邮件地址获取用户对象.
     * @param username - 用户名
     * @return 预期的用户对象或空引用
     */
    @Select("SELECT * FROM voj_users WHERE email = #{email}")
    @Options(useCache = true)
    @Results(value = {
        @Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.web.mapper.UserGroupMapper.getUserGroupUsingId")),
        @Result(property = "preferLanguage", column = "prefer_language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.web.mapper.LanguageMapper.getLanguageUsingId"))
    })
    public User getUserUsingEmail(@Param("email") String email);
    
    /**
     * [此方法仅供管理员使用]
     * 获取某个用户组中的用户列表.
     * @param userGroup - 用户所属的用户组对象
     * @param offset - 用户唯一标识符的起始编号
     * @param limit - 需要获取的用户的数量
     * @return 用户列表
     */
    @Select("SELECT * FROM voj_users WHERE user_group_id = #{userGroup.userGroupId} AND uid >= #{uid} LIMIT #{limit}")
    @Options(useCache = true)
    @Results(value = {
        @Result(property = "userGroup", column = "user_group_id", javaType=UserGroup.class, one = @One(select="com.trunkshell.voj.web.mapper.UserGroupMapper.getUserGroupUsingId")),
        @Result(property = "preferLanguage", column = "prefer_language_id", javaType=Language.class, one = @One(select="com.trunkshell.voj.web.mapper.LanguageMapper.getLanguageUsingId"))
    })
    public List<User> getUserUsingUserGroup(@Param("userGroup") UserGroup userGroup, @Param("uid") long offset, @Param("limit") int limit);
    
    /**
     * 创建新用户对象.
     * @param user - 待创建的用户对象
     */
    @Insert("INSERT INTO voj_users (username, password, email, user_group_id, prefer_language_id) VALUES (#{username}, #{password}, #{email}, #{userGroup.userGroupId}, #{preferLanguage.languageId})")
    @Options(useGeneratedKeys = true, keyProperty = "uid", keyColumn = "uid", flushCache = true)
    public void createUser(User user);
    
    /**
     * 更新用户对象.
     * @param user - 待更新信息的用户对象
     */
    @Update("UPDATE voj_users SET username = #{username}, password = #{password}, email = #{email}, user_group_id = #{userGroup.userGroupId}, prefer_language_id = #{preferLanguage.languageId} WHERE uid = #{uid}")
    @Options(flushCache = true)
    public void updateUser(User user);
    
    /**
     * 删除用户对象.
     * @param uid - 待删除用户的用户唯一标识符
     */
    @Delete("DELETE FROM voj_users WHERE uid = #{uid}")
    @Options(flushCache = true)
    public void deleteUser(@Param("uid") long uid);
}