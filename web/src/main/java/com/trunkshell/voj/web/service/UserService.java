package com.trunkshell.voj.web.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.trunkshell.voj.web.mapper.LanguageMapper;
import com.trunkshell.voj.web.mapper.UserGroupMapper;
import com.trunkshell.voj.web.mapper.UserMapper;
import com.trunkshell.voj.web.mapper.UserMetaMapper;
import com.trunkshell.voj.web.model.Language;
import com.trunkshell.voj.web.model.User;
import com.trunkshell.voj.web.model.UserGroup;
import com.trunkshell.voj.web.model.UserMeta;
import com.trunkshell.voj.web.util.DigestUtils;
import com.trunkshell.voj.web.util.HtmlTextFilter;
import com.trunkshell.voj.web.util.SensitiveWordFilter;

/**
 * 用户类(User)的业务逻辑层.
 * 
 * @author Xie Haozhe
 */
@Service
@Transactional
public class UserService {
    /**
     * 通过用户唯一标识符获取用户对象.
     * @param userId - 用户唯一标识符
     * @return 预期的用户对象或空引用
     */
    public User getUserUsingUid(long userId) {
        return userMapper.getUserUsingUid(userId);
    }
    
    /**
     * 获取用户的元信息.
     * @param user
     * @return
     */
    public Map<String, Object> getUserMetaUsingUid(User user) {
        List<UserMeta> userMetaList = userMetaMapper.getUserMetaUsingUser(user);
        Map<String, Object> userMetaMap = new HashMap<String, Object>(); 
        
        for ( UserMeta userMeta : userMetaList ) {
            String key = userMeta.getMetaKey();
            Object value = userMeta.getMetaValue();
            
            if ( "socialLinks".equals(key) ) {
                value = JSON.parseObject((String)value);
            }
            userMetaMap.put(key, value);
        }
        return userMetaMap;
    }
    
    /**
     * 通过用户名或电子邮件地址获取用户对象.
     * @param username - 用户名或电子邮件地址
     * @return 一个User对象或空引用
     */
    public User getUserUsingUsernameOrEmail(String username) {
        boolean isUsingEmail = username.indexOf('@') != -1;
        User user = null;
            
        if ( !isUsingEmail ) {
            user = userMapper.getUserUsingUsername(username);
        } else {
            user = userMapper.getUserUsingEmail(username);
        }
        return user;
    }
    
    /**
     * 验证用户身份是否有效.
     * @param username - 用户名或电子邮件地址
     * @param password - 密码(已使用MD5加密)
     * @return 一个包含登录验证结果的Map<String, Boolean>对象
     */
    public Map<String, Boolean> isAccountValid(String username, String password) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(5, 1);
        result.put("isUsernameEmpty", username.isEmpty());
        result.put("isPasswordEmpty", password.isEmpty());
        result.put("isAccountValid", false);
        result.put("isSuccessful", false);

        if ( !result.get("isUsernameEmpty") && !result.get("isPasswordEmpty") ) {
            User user = getUserUsingUsernameOrEmail(username);
            if ( user != null && user.getPassword().equals(password) && 
                !user.getUserGroup().getUserGroupSlug().equals("judgers") ) {
                result.put("isAccountValid", true);
                result.put("isSuccessful", true);
            }
        }
        return result;
    }
    
    /**
     * 验证账户有效性并创建用户.
     * @param username - 用户名
     * @param password - 密码(未使用MD5加密)
     * @param email - 电子邮件地址
     * @param languageSlug - 偏好语言的唯一英文缩写
     * @param isCsrfTokenValid - CSRF的Token是否正确
     * @param isAllowRegister - 系统是否允许注册新用户
     * @return 一个包含账户创建结果的Map<String, Boolean>对象
     */
    public Map<String, Boolean> createUser(String username, String password, String email, 
            String languageSlug, boolean isCsrfTokenValid, boolean isAllowRegister) {
        UserGroup userGroup = getUserGroupUsingSlug("users");
        Language languagePreference = languageMapper.getLanguageUsingSlug(languageSlug);
        User user = new User(username, DigestUtils.md5Hex(password), email, userGroup, languagePreference);
        
        Map<String, Boolean> result = getUserCreationResult(user, password, isCsrfTokenValid, isAllowRegister);
        if ( result.get("isSuccessful") ) {
            userMapper.createUser(user);
            createUserMeta(user);
        }
        return result;
    }
    
    /**
     * 创建用户元信息.
     * @param user - 对应的用户对象
     */
    private void createUserMeta(User user) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        String registerTime = formatter.format(calendar.getTime());
        UserMeta registerTimeMeta = new UserMeta(user, "RegisterTime", registerTime);
        userMetaMapper.createUserMeta(registerTimeMeta);
    }
    
    /**
     * 验证待创建用户信息的合法性.
     * @param user - 待创建的User对象
     * @param password - 密码(未使用MD5加密)
     * @param isCsrfTokenValid - CSRF的Token是否正确
     * @param isAllowRegister - 系统是否允许注册新用户
     * @return 一个包含账户信息验证结果的Map<String, Boolean>对象
     */
    private Map<String, Boolean> getUserCreationResult(User user, String password, 
            boolean isCsrfTokenValid, boolean isAllowRegister) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(12, 1);
        result.put("isUsernameEmpty", user.getUsername().isEmpty());
        result.put("isUsernameLegal", isUsernameLegal(user.getUsername()));
        result.put("isUsernameExists", isUsernameExists(user.getUsername()));
        result.put("isPasswordEmpty", password.isEmpty());
        result.put("isPasswordLegal", isPasswordLegal(password));
        result.put("isEmailEmpty", user.getEmail().isEmpty());
        result.put("isEmailLegal", isEmailLegal(user.getEmail()));
        result.put("isEmailExists", isEmailExists(user.getEmail()));
        result.put("isLanguageLegal", user.getPreferLanguage() != null);
        result.put("isCsrfTokenValid", isCsrfTokenValid);
        result.put("isAllowRegister", isAllowRegister);
        
        boolean isSuccessful = !result.get("isUsernameEmpty")  &&  result.get("isUsernameLegal")  &&
                               !result.get("isUsernameExists") && !result.get("isPasswordEmpty")  &&
                                result.get("isPasswordLegal")  && !result.get("isEmailEmpty")     &&
                                result.get("isEmailLegal")     && !result.get("isEmailExists")    &&
                                result.get("isLanguageLegal")  &&  result.get("isCsrfTokenValid") &&
                                result.get("isAllowRegister");
        result.put("isSuccessful", isSuccessful);
        return result;
    }
    
    /**
     * 验证旧密码正确性并修改密码.
     * @param user - 待修改密码的用户对象
     * @param oldPassword - 旧密码
     * @param newPassword - 新密码
     * @param confirmPassword - 确认新密码
     * @return 一个包含密码验证结果的Map<String, Boolean>对象
     */
    public Map<String, Boolean> changePassword(User user, String oldPassword, 
            String newPassword, String confirmPassword) {
        Map<String, Boolean> result = getChangePasswordResult(user, oldPassword, newPassword, confirmPassword);
        
        if ( result.get("isSuccessful") ) {
            user.setPassword(DigestUtils.md5Hex(newPassword));
            userMapper.updateUser(user);
        }
        return result;
    }
    
    /**
     * 验证旧密码的正确性和新密码的合法性.
     * @param user - 待修改密码的用户对象
     * @param oldPassword - 旧密码
     * @param newPassword - 新密码
     * @param confirmPassword - 确认新密码
     * @return 一个包含密码验证结果的Map<String, Boolean>对象
     */
    private Map<String, Boolean> getChangePasswordResult(User user, String oldPassword, 
            String newPassword, String confirmPassword) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(5, 1);
        result.put("isOldPasswordCorrect", isOldPasswordCorrect(user.getPassword(), oldPassword));
        result.put("isNewPasswordEmpty", newPassword.isEmpty());
        result.put("isNewPasswordLegal", isPasswordLegal(newPassword));
        result.put("isConfirmPasswordMatched", newPassword.equals(confirmPassword));
        
        boolean isSuccessful = result.get("isOldPasswordCorrect") && !result.get("isNewPasswordEmpty") &&
                               result.get("isNewPasswordLegal")   &&  result.get("isConfirmPasswordMatched");
        
        result.put("isSuccessful", isSuccessful);
        return result;
    }
    
    /**
     * 验证新资料的有效性并更新个人资料.
     * @param user - 待更改资料的用户
     * @param email - 用户的电子邮件地址
     * @param location - 用户的所在地区
     * @param website - 用户的个人主页
     * @param socialLinks - 用户的社交网络信息
     * @param aboutMe - 用户的个人简介
     * @return 一个包含个人资料修改结果的Map<String, Boolean>对象
     */
    public Map<String, Boolean> updateProfile(User user, String email, 
            String location, String website, String socialLinks, String aboutMe) {
        location = HtmlTextFilter.filter(location);
        website = HtmlTextFilter.filter(website);
        socialLinks = HtmlTextFilter.filter(socialLinks);
        aboutMe = sensitiveWordFilter.filter(HtmlTextFilter.filter(aboutMe));
        Map<String, Boolean> result = getUpdateProfileResult(user, email, location, website, socialLinks, aboutMe);
        
        if ( result.get("isSuccessful") ) {
            user.setEmail(email);
            userMapper.updateUser(user);
            
            updateUserMeta(user, "location", location);
            updateUserMeta(user, "website", website);
            updateUserMeta(user, "socialLinks", socialLinks);
            updateUserMeta(user, "aboutMe", aboutMe);
        }
        return result;
    }
    
    /**
     * 验证新资料的有效性.
     * @param user - 待更改资料的用户
     * @param email - 用户的电子邮件地址
     * @param location - 用户的所在地区
     * @param website - 用户的个人主页
     * @param socialLinks - 用户的社交网络信息
     * @param aboutMe - 用户的个人简介
     * @return 一个包含个人资料修改结果的Map<String, Boolean>对象
     */
    private Map<String, Boolean> getUpdateProfileResult(User user, String email, 
            String location, String website, String socialLinks, String aboutMe) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(12, 1);
        result.put("isEmailEmpty", email.isEmpty());
        result.put("isEmailLegal", isEmailLegal(email));
        result.put("isEmailExists", isEmailExists(user.getEmail(), email));
        result.put("isLocationLegal", location.length() <= 128);
        result.put("isWebsiteLegal", isWebsiteLegal(website));
        result.put("isAboutMeLegal", aboutMe.length() <= 256);
        
        boolean isSuccessful = !result.get("isEmailEmpty")   && result.get("isEmailLegal")    &&
                               !result.get("isEmailExists")  && result.get("isLocationLegal") &&
                                result.get("isWebsiteLegal") && result.get("isAboutMeLegal");
        
        result.put("isSuccessful", isSuccessful);
        return result;
    }
    
    /**
     * 更新用户元信息.
     * @param user - 待更新元信息的用户
     * @param metaKey - 元信息的键
     * @param metaValue - 元信息的值
     */
    private void updateUserMeta(User user, String metaKey, String metaValue) {
        UserMeta userMeta = userMetaMapper.getUserMetaUsingUserAndMetaKey(user, metaKey);
        
        if ( userMeta == null ) {
            if ( metaValue.isEmpty() ) {
                return;
            }
            userMeta = new UserMeta(user, metaKey, metaValue);
            userMetaMapper.createUserMeta(userMeta);
        } else {
            userMeta.setMetaValue(metaValue);
            userMetaMapper.updateUserMeta(userMeta);
        }
    }
    
    /**
     * 验证用户名的合法性:
     * 规则: 用户名应由[A-Za-z0-9_]组成, 以字母起始且长度在6-16个字符.
     * @param username - 用户名
     * @return 用户名是否合法
     */
    private boolean isUsernameLegal(String username) {
        return username.matches("^[A-Za-z][A-Za-z0-9_]{5,15}$");
    }
    
    /**
     * 检查用户名是否存在.
     * @param username - 用户名
     * @return 用户名是否存在
     */
    private boolean isUsernameExists(String username) {
        User user = userMapper.getUserUsingUsername(username);
        return user != null;
    }
    
    /**
     * 检查密码是否合法.
     * 规则: 密码的长度在6-16个字符.
     * @param password - 密码(未经MD5加密)
     * @return 密码是否合法
     */
    private boolean isPasswordLegal(String password) {
        int passwordLength = password.length();
        return passwordLength >= 6 && passwordLength <= 16;
    }
    
    /**
     * 更改密码时, 验证用户的旧密码是否正确.
     * @param oldPassword - 用户的旧密码(已使用MD5加密)
     * @param submitedPassword - 所提交进行验证的旧密码(未使用MD5加密)
     * @return 用户旧密码是否正确
     */
    private boolean isOldPasswordCorrect(String oldPassword, String submitedPassword) {
        if ( submitedPassword.isEmpty() ) {
            return true;
        }
        return oldPassword.equals(DigestUtils.md5Hex(submitedPassword));
    }
    
    /**
     * 检查电子邮件地址是否合法.
     * 规则: 合法的电子邮件地址且长度不超过64个字符.
     * @param email - 电子邮件地址
     * @return 电子邮件地址是否合法
     */
    private boolean isEmailLegal(String email) {
        int emailLength = email.length();
        return emailLength <= 64 && email.matches("^[A-Za-z0-9\\._-]+@[A-Za-z0-9_-]+\\.[A-Za-z0-9\\._-]+$");
    }
    
    /**
     * 检查电子邮件地址是否存在.
     * 说明: 仅用于用户创建新账户
     * @param email - 电子邮件地址
     * @return 电子邮件地址是否存在
     */
    private boolean isEmailExists(String email) {
        User user = userMapper.getUserUsingEmail(email);
        return user != null;
    }
    
    /**
     * 检查电子邮件地址是否存在.
     * 说明: 仅用于用户编辑个人资料
     * @param currentEmail - 之前所使用的Email地址
     * @param email - 待更新的Email地址
     * @return 电子邮件地址是否存在
     */
    private boolean isEmailExists(String currentEmail, String email) {
        if ( currentEmail.equals(email) ) {
            return false;
        }
        User user = userMapper.getUserUsingEmail(email);
        return user != null;
    }
    
    /**
     * 检查个人主页的地址是否合法.
     * 规则: 合法的HTTP(S)协议URL且长度不超过64个字符.
     * @param website - 个人主页的地址
     * @return 个人主页的地址是否合法
     */
    private boolean isWebsiteLegal(String website) {
        int websiteLength = website.length();
        return website.isEmpty() || 
              (websiteLength <= 64 && website.matches("^(http|https):\\/\\/[A-Za-z0-9-]+\\.[A-Za-z0-9_.]+$"));
    }
    
    /**
     * 通过用户组的唯一英文缩写获取用户组对象.
     * @param userGroupSlug - 用户组的唯一英文缩写
     * @return 用户组对象或空引用
     */
    public UserGroup getUserGroupUsingSlug(String userGroupSlug) {
        UserGroup userGroup = userGroupMapper.getUserGroupUsingSlug(userGroupSlug);
        return userGroup;
    }
    
    /**
     * [此方法仅供管理员使用]
     * 获取系统中注册用户的总数.
     * @param userGroup - 用户所属的用户组对象
     * @return 系统中注册用户的总数
     */
    public long getNumberOfUsers(UserGroup userGroup) {
        return userMapper.getNumberOfUsersUsingUserGroup(userGroup);
    }
    
    /**
     * [此方法仅供管理员使用]
     * 获取今日注册的用户数量.
     * @return 今日注册的用户数量
     */
    public long getNumberOfUserRegisteredToday() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DAY_OF_MONTH);
        
        calendar.set(year, month, date, 0, 0, 0);
        Date startTime = calendar.getTime();
        calendar.set(year, month, date, 23, 59, 59);
        Date endTime = calendar.getTime();
        
        return userMetaMapper.getNumberOfUserRegistered(startTime, endTime);
    }
    
    /**
     * 自动注入的UserMapper对象.
     */
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 自动注入的UserMetaMapper对象.
     */
    @Autowired
    private UserMetaMapper userMetaMapper;
    
    /**
     * 自动注入的UserGroupMapper对象.
     */
    @Autowired
    private UserGroupMapper userGroupMapper;
    
    /**
     * 自动注入的LanguageMapper对象.
     */
    @Autowired
    private LanguageMapper languageMapper;
    
    /**
     * 自动注入的SensitiveWordFilter对象.
     * 用于过滤用户个人信息中的敏感词.
     */
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;    
}