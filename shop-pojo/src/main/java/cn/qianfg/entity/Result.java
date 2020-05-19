package cn.qianfg.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

//返回值
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Result implements Serializable {
    //是否成功
    private Boolean success;
    //返回信息
    private String message;
    //状态码
    private Integer code;
}
