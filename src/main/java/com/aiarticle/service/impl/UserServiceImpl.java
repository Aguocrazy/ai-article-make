package com.aiarticle.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aiarticle.constant.UserConstant;
import com.aiarticle.exception.BusinessException;
import com.aiarticle.exception.ErrorCode;
import com.aiarticle.mapper.UserMapper;
import com.aiarticle.model.dto.user.UserAddRequest;
import com.aiarticle.model.dto.user.UserLoginRequest;
import com.aiarticle.model.dto.user.UserQueryRequest;
import com.aiarticle.model.dto.user.UserRegisterRequest;
import com.aiarticle.model.entity.User;
import com.aiarticle.model.vo.LoginUserVO;
import com.aiarticle.model.vo.UserVO;
import com.aiarticle.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.aiarticle.model.entity.table.UserTableDef.USER;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * 应用级固定盐（pepper），不入库；与每人一盐叠加，避免通用彩虹表直接命中
     */
    private static final String PEPPER = "guoshao";

    private static final int ACCOUNT_MIN_LEN = 4;
    private static final int ACCOUNT_MAX_LEN = 256;
    private static final int PASSWORD_MIN_LEN = 8;
    private static final int PASSWORD_MAX_LEN = 512;

    /**
     * Session 中登录用户的 key
     */
    private static final String USER_LOGIN_STATE = "userLoginState";

    @Resource
    private UserMapper userMapper;

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        // 1. 参数校验（账号 / 密码长度）
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < ACCOUNT_MIN_LEN || userAccount.length() > ACCOUNT_MAX_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度必须在 4 到 256 位之间");
        }
        if (userPassword.length() < PASSWORD_MIN_LEN || userPassword.length() > PASSWORD_MAX_LEN
                || checkPassword.length() < PASSWORD_MIN_LEN || checkPassword.length() > PASSWORD_MAX_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在 8 到 512 位之间");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2. 查重：先查库；并发下仍可能撞车，插入时靠 uk_userAccount 兜底
        long count = userMapper.selectCountByQuery(QueryWrapper.create().where(USER.USER_ACCOUNT.eq(userAccount)));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        // 3. 加密：每人随机盐 + 应用 pepper，摘要格式 盐$MD5(明文+盐+pepper)
        String encryptPassword = encodePassword(userPassword);

        // 4. 插入数据
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName(userAccount)
                .userRole(UserConstant.DEFAULT_ROLE)
                .build();
        try {
            boolean saved = userMapper.insert(user) > 0;
            if (!saved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        // 1. 参数校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < ACCOUNT_MIN_LEN || userAccount.length() > ACCOUNT_MAX_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < PASSWORD_MIN_LEN || userPassword.length() > PASSWORD_MAX_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        // 2. 按账号查出用户后用对应盐校验（不能把摘要直接放进 WHERE，每人一盐）
        User user = userMapper.selectOneByQuery(QueryWrapper.create()
                .where(USER.USER_ACCOUNT.eq(userAccount)));
        if (user == null || !matchesPassword(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }

        // 3. 保存登录态到 Session（Spring Session 自动存储到 Redis）
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查最新数据（保证数据新鲜度）
        long userId = currentUser.getId();
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    @Override
    public LoginUserVO getLoginUserVO(HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        return this.getLoginUserVO(loginUser);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public long addUser(UserAddRequest userAddRequest) {
        String userAccount = userAddRequest.getUserAccount();
        String userPassword = userAddRequest.getUserPassword();
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < ACCOUNT_MIN_LEN || userAccount.length() > ACCOUNT_MAX_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度必须在 4 到 256 位之间");
        }
        if (userPassword.length() < PASSWORD_MIN_LEN || userPassword.length() > PASSWORD_MAX_LEN) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在 8 到 512 位之间");
        }
        String encryptPassword = encodePassword(userPassword);

        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        user.setUserPassword(encryptPassword);
        try {
            boolean saved = userMapper.insert(user) > 0;
            if (!saved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建用户失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        return user.getId();
    }

    @Override
    public boolean update(User user) {
        return userMapper.update(user) > 0;
    }

    @Override
    public boolean removeById(long id) {
        // 实体配置了逻辑删除，deleteById 实际执行 UPDATE isDelete = 1
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();

        // 构造查询条件
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (userQueryRequest.getId() != null) {
            queryWrapper.where(USER.ID.eq(userQueryRequest.getId()));
        }
        if (StrUtil.isNotBlank(userQueryRequest.getUserAccount())) {
            queryWrapper.where(USER.USER_ACCOUNT.like(userQueryRequest.getUserAccount()));
        }
        if (StrUtil.isNotBlank(userQueryRequest.getUserName())) {
            queryWrapper.where(USER.USER_NAME.like(userQueryRequest.getUserName()));
        }
        if (StrUtil.isNotBlank(userQueryRequest.getUserProfile())) {
            queryWrapper.where(USER.USER_PROFILE.like(userQueryRequest.getUserProfile()));
        }
        if (StrUtil.isNotBlank(userQueryRequest.getUserRole())) {
            queryWrapper.where(USER.USER_ROLE.eq(userQueryRequest.getUserRole()));
        }
        queryWrapper.orderBy(USER.CREATE_TIME.desc());

        // 分页查询
        Page<User> userPage = userMapper.paginate(current, pageSize, queryWrapper);
        // 转换为脱敏 VO
        List<UserVO> userVOList = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            userVOList.add(this.getUserVO(user));
        }
        Page<UserVO> userVOPage = new Page<>(userVOList, userPage.getPageNumber(), userPage.getPageSize(), userPage.getTotalRow());
        return userVOPage;
    }

    /**
     * 实体转脱敏 VO
     */
    private UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 实体转登录 VO（脱敏）
     */
    private LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 新密码：随机盐 + 摘要，存为 {@code salt$hash}
     */
    private String encodePassword(String rawPassword) {
        String salt = IdUtil.fastSimpleUUID();
        return salt + "$" + digest(rawPassword, salt);
    }

    /**
     * 校验明文。含 {@code $} 的走每人一盐；否则兼容种子数据 MD5(明文 + pepper)。
     */
    private boolean matchesPassword(String rawPassword, String stored) {
        if (StrUtil.isBlank(stored)) {
            return false;
        }
        int sep = stored.indexOf('$');
        if (sep > 0) {
            String salt = stored.substring(0, sep);
            String hash = stored.substring(sep + 1);
            return digest(rawPassword, salt).equals(hash);
        }
        return DigestUtils.md5DigestAsHex((rawPassword + PEPPER).getBytes(StandardCharsets.UTF_8)).equals(stored);
    }

    private String digest(String rawPassword, String salt) {
        return DigestUtils.md5DigestAsHex((rawPassword + salt + PEPPER).getBytes(StandardCharsets.UTF_8));
    }
}