package com.smartdx.property.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class JobErrorPageVO {

    private long total;

    private int page;

    private int size;

    private List<JobErrorVO> errors = new ArrayList<>();
}
