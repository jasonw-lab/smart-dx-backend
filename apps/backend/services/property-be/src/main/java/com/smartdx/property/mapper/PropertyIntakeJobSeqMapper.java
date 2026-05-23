package com.smartdx.property.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdx.property.model.entity.PropertyIntakeJobSeq;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PropertyIntakeJobSeqMapper extends BaseMapper<PropertyIntakeJobSeq> {

    @Select("""
            SELECT id,
                   tenant_id,
                   job_type,
                   DATE_FORMAT(seq_date, '%Y%m%d') AS date_part,
                   current_seq AS seq,
                   update_time
            FROM property_intake_job_seq
            WHERE tenant_id = #{tenantId}
              AND job_type = #{jobType}
              AND seq_date = STR_TO_DATE(#{datePart}, '%Y%m%d')
            FOR UPDATE
            """)
    PropertyIntakeJobSeq selectForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("jobType") String jobType,
            @Param("datePart") String datePart
    );

    @Insert("""
            INSERT INTO property_intake_job_seq(tenant_id, job_type, seq_date, current_seq, update_time)
            VALUES(#{tenantId}, #{jobType}, STR_TO_DATE(#{datePart}, '%Y%m%d'), #{seq}, NOW())
            """)
    int insertSeq(PropertyIntakeJobSeq seq);

    @Update("""
            UPDATE property_intake_job_seq
            SET current_seq = #{seq}, update_time = NOW()
            WHERE id = #{id}
            """)
    int updateSeq(PropertyIntakeJobSeq seq);
}
