package com.github.liuyuyu.dictator.server.web.service;

import com.github.liuyuyu.dictator.common.utils.BeanConverter;
import com.github.liuyuyu.dictator.common.utils.UUIDUtils;
import com.github.liuyuyu.dictator.server.mapper.DictatorResourceMapper;
import com.github.liuyuyu.dictator.server.mapper.DictatorRoleMapper;
import com.github.liuyuyu.dictator.server.mapper.DictatorUserMapper;
import com.github.liuyuyu.dictator.server.mapper.DictatorUserRoleMapper;
import com.github.liuyuyu.dictator.server.model.entity.DictatorRole;
import com.github.liuyuyu.dictator.server.model.entity.DictatorUser;
import com.github.liuyuyu.dictator.server.model.entity.DictatorUserRole;
import com.github.liuyuyu.dictator.server.utils.J8Converter;
import com.github.liuyuyu.dictator.server.web.annotation.TransactionalAutoRollback;
import com.github.liuyuyu.dictator.server.web.constant.UserConstants;
import com.github.liuyuyu.dictator.server.web.exception.ServiceException;
import com.github.liuyuyu.dictator.server.web.exception.enums.UserErrorMessageEnum;
import com.github.liuyuyu.dictator.server.web.model.dto.DictatorResourceDto;
import com.github.liuyuyu.dictator.server.web.model.dto.DictatorUserDto;
import com.github.liuyuyu.dictator.server.web.model.dto.DictatorUserRoleDto;
import com.github.liuyuyu.dictator.server.web.model.param.DictatorUserSaveOrUpdateParam;
import com.github.liuyuyu.dictator.server.web.model.param.LoginParam;
import com.github.liuyuyu.dictator.server.web.model.param.UpdatePasswordParam;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author liuyuyu
 */
@Service
public class UserService {
    @Autowired
    private DictatorUserMapper userMapper;
    @Autowired
    private DictatorResourceMapper resourceMapper;
    @Autowired
    private DictatorRoleMapper roleMapper;
    @Autowired private DictatorUserRoleMapper userRoleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired private ResourceService resourceService;
    @Autowired private RoleService roleService;

    public DictatorUserDto login(@NonNull LoginParam loginParam) {
        //一个账号都没有的时候，启用GM账号
        int hasUser = this.userMapper.countAll();
        if(hasUser < 1) {
            return this.loginGM();
        }
        //用户校验
        Optional<DictatorUser> userOptional = this.userMapper.findByUsername(loginParam.getUsername());
        DictatorUser dictatorUser = userOptional.orElseThrow(() -> ServiceException.from(UserErrorMessageEnum.USER_NOT_FOUND));
        //密码校验
        if (StringUtils.isNotBlank(dictatorUser.getPassword()) && !this.passwordEncoder.matches(loginParam.getPassword(),dictatorUser.getPassword())) {
            throw ServiceException.from(UserErrorMessageEnum.INCORRECT_PASSWORD);
        }
        DictatorUserDto dictatorUserDto = new DictatorUserDto();
        dictatorUserDto.from(dictatorUser);
        dictatorUserDto.setToken(UUIDUtils.next());
        List<Long> roleIdList = this.roleMapper.findByUserId(dictatorUser.getId()).stream()
                .map(DictatorRole::getId)
                .collect(Collectors.toList());
        dictatorUserDto.setRoleIdList(roleIdList);
        return dictatorUserDto;
    }

    private DictatorUserDto loginGM(){
        DictatorUserDto dictatorUserDto = new DictatorUserDto();
        dictatorUserDto.setToken(UUIDUtils.next());
        dictatorUserDto.setUserName(UserConstants.GM_ACCOUNT);
        dictatorUserDto.setId(-1L);
        return dictatorUserDto;
    }

    public DictatorUserDto findUserInfo(@NonNull Long userId) {
        //如果启用了GM账号
        if(Objects.equals(UserConstants.GM_USER_ID, userId)){
            DictatorUserDto dictatorUserDto = this.loginGM();
            List<DictatorResourceDto> dictatorResourceDtos = BeanConverter.from(this.resourceMapper.findAll())
                    .toList(DictatorResourceDto.class);
            dictatorUserDto.setResourceList(dictatorResourceDtos);
            return dictatorUserDto;
        }
        DictatorUser dictatorUser = this.userMapper.selectByPrimaryKey(userId);
        if (dictatorUser != null) {
            DictatorUserDto dictatorUserDto = new DictatorUserDto();
            dictatorUserDto.from(dictatorUser);
            List<Long> roleIdList = this.roleMapper.findByUserId(dictatorUserDto.getId()).stream()
                    .map(DictatorRole::getId)
                    .collect(Collectors.toList());
            if (!roleIdList.isEmpty()) {
                List<DictatorResourceDto> resourceList = BeanConverter.from(this.resourceMapper.findByRoleIdList(roleIdList))
                        .toList(DictatorResourceDto.class);

                dictatorUserDto.setResourceList(resourceList);
            }
            return dictatorUserDto;
        } else {
            return null;
        }
    }

    @TransactionalAutoRollback
    public void saveOrUpdate(DictatorUserSaveOrUpdateParam userSaveOrUpdateParam) {
        DictatorUser userEntity = userSaveOrUpdateParam.to(DictatorUser.class);
        if (userSaveOrUpdateParam.getId() == null) {
            userEntity.setPassword(this.genDefaultPassword());
            this.userMapper.insertSelective(userEntity);
        } else {
            this.userMapper.updateByPrimaryKeySelective(userEntity);
        }
        //角色处理
        this.userRoleMapper.deleteByUserId(userSaveOrUpdateParam.getId());
        userSaveOrUpdateParam.getRoleIdList().stream()
                .map(roleId->{
                    DictatorUserRole userRoleEntity = new DictatorUserRole();
                    userRoleEntity.setRoleId(roleId);
                    userRoleEntity.setUserId(userEntity.getId());
                    return userRoleEntity;
                })
                .forEach(ur-> this.userRoleMapper.insertSelective(ur));
    }

    public String genDefaultPassword(){
        return this.passwordEncoder.encode("123456");
    }

    public List<DictatorUserDto> findAll() {
        List<DictatorUserDto> dictatorUserDtoList = this.userMapper.findAllSummary();
        List<Long> userIdList = dictatorUserDtoList.stream()
                .map(DictatorUserDto::getId)
                .collect(Collectors.toList());
        if(!userIdList.isEmpty()){
            List<DictatorUserRoleDto> userRoleDtoList = this.roleMapper.findByUserIdList(userIdList);
            Map<Long, List<DictatorUserRoleDto>> userIdRoleMap = new J8Converter<DictatorUserRoleDto, Long>(userRoleDtoList)
                    .toMap(DictatorUserRoleDto::getUserId);
            return dictatorUserDtoList.stream()
                    .peek(u->{
                        List<DictatorUserRoleDto> dictatorUserRoleDtos = userIdRoleMap.get(u.getId());
                        if(dictatorUserRoleDtos == null){
                            return;
                        }
                        List<Long> roleIdList = dictatorUserRoleDtos.stream()
                                .map(DictatorUserRoleDto::getRoleId)
                                .collect(Collectors.toList());
                        u.setRoleIdList(roleIdList);
                    })
                    .collect(Collectors.toList());
        }
        return dictatorUserDtoList;
    }

    @TransactionalAutoRollback
    public void updatePassword(@NonNull UpdatePasswordParam updatePasswordParam) {
        //用户校验
        final DictatorUser dictatorUser = this.userMapper.findById(updatePasswordParam.getUserId())
                .orElseThrow(UserErrorMessageEnum.UNKNOWN_USER::serviceException);
        //旧密码校验
        final boolean isValidOldPassword = this.passwordEncoder.matches(updatePasswordParam.getOldPassword(), dictatorUser.getPassword());
        if(!isValidOldPassword){
            throw UserErrorMessageEnum.INCORRECT_OLD_PASSWORD.serviceException();
        }
        //确认密码验证
        if(!StringUtils.equals(updatePasswordParam.getNewPassword(),updatePasswordParam.getConfirmPassword())){
            throw UserErrorMessageEnum.INCORRECT_CONFIRM_PASSWORD.serviceException();
        }
        final String encodedNewPassword = this.passwordEncoder.encode(updatePasswordParam.getNewPassword());
        this.userMapper.updatePasswordById(updatePasswordParam.getUserId(),encodedNewPassword);
    }

    @TransactionalAutoRollback
    public void deleteById(@NonNull Long userId) {
        this.userRoleMapper.deleteByUserId(userId);
        this.userMapper.deleteByPrimaryKey(userId);
    }
}
