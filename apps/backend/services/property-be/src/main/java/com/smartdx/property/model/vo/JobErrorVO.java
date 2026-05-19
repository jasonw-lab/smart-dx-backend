package com.smartdx.property.model.vo;

import lombok.Data;

@Data
public class JobErrorVO {

    private String assetKey;

    private String docKey;

    private Integer rowNo;

    private String errorCode;

    private String message;

    private String fileName;
}
