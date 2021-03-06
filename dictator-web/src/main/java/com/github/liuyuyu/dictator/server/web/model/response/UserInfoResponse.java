package com.github.liuyuyu.dictator.server.web.model.response;

import com.github.liuyuyu.dictator.common.Convertible;
import com.github.liuyuyu.dictator.common.Resolvable;
import com.github.liuyuyu.dictator.server.web.model.dto.DictatorResourceDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liuyuyu
 */
@Data
public class UserInfoResponse implements Convertible,Resolvable {
    /**
     * 用户名
     */
    private String name;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 拥有资源
     */
    private List<DictatorResourceDto> resourceList = new ArrayList<>();

    public static UserInfoResponse of() {
        return new UserInfoResponse();
    }
}
