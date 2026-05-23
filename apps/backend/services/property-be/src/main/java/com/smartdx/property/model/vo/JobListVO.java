package com.smartdx.property.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class JobListVO {

    private long total;

    private int page;

    private int size;

    private Map<String, Long> summary = new LinkedHashMap<>();

    private List<JobListItemVO> jobs = new ArrayList<>();
}
