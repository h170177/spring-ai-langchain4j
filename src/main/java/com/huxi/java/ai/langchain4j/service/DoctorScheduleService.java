package com.huxi.java.ai.langchain4j.service;

import com.huxi.java.ai.langchain4j.entity.DoctorSchedule;

import java.util.List;
import java.util.Map;

public interface DoctorScheduleService {

    /**
     * 查询指定科室在指定日期时段的号源情况。
     * 返回 String 供 LLM 直接使用。
     *
     * @param department 科室名称
     * @param date       日期 (格式: 2025-04-14)
     * @param timeSlot   时段 (上午/下午)
     * @param doctorName 可选医生姓名，为 null 时查询该科室全部可约医生
     */
    String queryAvailability(String department, String date,
                             String timeSlot, String doctorName);

    /**
     * 预约成功后扣减一位号源。
     * @return true 扣减成功，false 号源已满或不存在
     */
    boolean decrementSlot(String doctorName, String date, String timeSlot);

    /**
     * 取消预约后释放一位号源。
     */
    void incrementSlot(String doctorName, String date, String timeSlot);
}
