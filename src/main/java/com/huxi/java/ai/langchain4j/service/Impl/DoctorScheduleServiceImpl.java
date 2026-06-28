package com.huxi.java.ai.langchain4j.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huxi.java.ai.langchain4j.entity.Doctor;
import com.huxi.java.ai.langchain4j.entity.DoctorSchedule;
import com.huxi.java.ai.langchain4j.mapper.DoctorMapper;
import com.huxi.java.ai.langchain4j.mapper.DoctorScheduleMapper;
import com.huxi.java.ai.langchain4j.service.DoctorScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;

    @Override
    public String queryAvailability(String department, String date,
                                    String timeSlot, String doctorName) {
        if (doctorName != null && !doctorName.isBlank()) {
            return queryByDoctor(department, date, timeSlot, doctorName.trim());
        }
        return queryByDepartment(department, date, timeSlot);
    }

    private String queryByDoctor(String department, String date,
                                  String timeSlot, String doctorName) {
        Doctor doctor = doctorMapper.selectOne(new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getName, doctorName)
                .eq(Doctor::getDepartment, department));
        if (doctor == null) {
            return department + "没有叫" + doctorName + "的医生";
        }
        DoctorSchedule schedule = findSchedule(doctor.getId(), date, timeSlot);
        if (schedule == null) {
            return doctorName + "医生（" + doctor.getTitle() + "）在" + date + " " + timeSlot + "不值班";
        }
        int remaining = schedule.getMaxPatients() - schedule.getBookedCount();
        if (remaining <= 0) {
            return doctorName + "医生（" + doctor.getTitle() + "）在" + date + " " + timeSlot + "号源已约满";
        }
        return doctorName + "医生（" + doctor.getTitle() + "，" + doctor.getSpecialty() + "）可预约，剩余" + remaining + "个号";
    }

    private String queryByDepartment(String department, String date, String timeSlot) {
        List<Doctor> deptDoctors = doctorMapper.selectList(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getDepartment, department));
        if (deptDoctors.isEmpty()) {
            return department + "暂无医生信息";
        }
        List<String> availableList = new ArrayList<>();
        List<String> fullList = new ArrayList<>();
        for (Doctor doc : deptDoctors) {
            DoctorSchedule schedule = findSchedule(doc.getId(), date, timeSlot);
            if (schedule == null) continue;
            int remaining = schedule.getMaxPatients() - schedule.getBookedCount();
            if (remaining > 0) {
                availableList.add(doc.getName() + "（" + doc.getTitle() + "，剩" + remaining + "个号）");
            } else {
                fullList.add(doc.getName() + "（已约满）");
            }
        }
        if (availableList.isEmpty() && fullList.isEmpty()) {
            return department + "在" + date + " " + timeSlot + "无医生值班";
        }
        StringBuilder sb = new StringBuilder();
        if (!availableList.isEmpty()) {
            sb.append("可预约：").append(String.join("、", availableList));
        }
        if (!fullList.isEmpty()) {
            if (sb.length() > 0) sb.append("；");
            sb.append("已约满：").append(String.join("、", fullList));
        }
        return sb.toString();
    }

    private DoctorSchedule findSchedule(Long doctorId, String date, String timeSlot) {
        return doctorScheduleMapper.selectOne(new LambdaQueryWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getDoctorId, doctorId)
                .eq(DoctorSchedule::getDate, date)
                .eq(DoctorSchedule::getTimeSlot, timeSlot));
    }

    @Override
    @Transactional
    public boolean decrementSlot(String doctorName, String date, String timeSlot) {
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getName, doctorName));
        if (doctor == null) return false;
        DoctorSchedule schedule = findSchedule(doctor.getId(), date, timeSlot);
        if (schedule == null) return false;
        if (schedule.getBookedCount() >= schedule.getMaxPatients()) return false;
        doctorScheduleMapper.update(null, new LambdaUpdateWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getId, schedule.getId())
                .lt(DoctorSchedule::getBookedCount, schedule.getMaxPatients())
                .setSql("booked_count = booked_count + 1"));
        return true;
    }

    @Override
    @Transactional
    public void incrementSlot(String doctorName, String date, String timeSlot) {
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getName, doctorName));
        if (doctor == null) return;
        doctorScheduleMapper.update(null, new LambdaUpdateWrapper<DoctorSchedule>()
                .eq(DoctorSchedule::getDoctorId, doctor.getId())
                .eq(DoctorSchedule::getDate, date)
                .eq(DoctorSchedule::getTimeSlot, timeSlot)
                .gt(DoctorSchedule::getBookedCount, 0)
                .setSql("booked_count = booked_count - 1"));
    }
}
