package com.huxi.java.ai.langchain4j.tools;

import com.huxi.java.ai.langchain4j.entity.Appointment;
import com.huxi.java.ai.langchain4j.service.AppointmentService;
import com.huxi.java.ai.langchain4j.service.DoctorScheduleService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("appointmentTools")
public class AppointmentTool {
    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Tool(name = "预约挂号", value = "根据参数，先执行工具方法queryDepartment查询是否可预约，" +
            "并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约。" +
            "预约成功后系统会自动扣减该医生对应时段的号源。")
    public String bookAppointment(Appointment appointment) {
        Appointment appointmentDB = appointmentService.getOne(appointment);
        if (appointmentDB == null) {
            appointment.setId(null);
            if (appointmentService.save(appointment)) {
                // 预约成功，扣减号源
                doctorScheduleService.decrementSlot(
                        appointment.getDoctorName(),
                        appointment.getDate(),
                        appointment.getTime());
                return "预约成功，并返回预约详情";
            } else {
                return "预约失败";
            }
        }
        return "您在相同的科室和时间已有预约";
    }

    @Tool(name = "取消预约挂号", value = "根据参数，查询预约是否存在，如果存在则删除预约记录，" +
            "并自动释放该医生对应时段的号源，返回取消预约成功，否则返回取消预约失败")
    public String cancelAppointment(Appointment appointment) {
        Appointment appointmentDB = appointmentService.getOne(appointment);
        if (appointmentDB != null) {
            if (appointmentService.removeById(appointmentDB.getId())) {
                // 取消成功，释放号源
                doctorScheduleService.incrementSlot(
                        appointment.getDoctorName(),
                        appointment.getDate(),
                        appointment.getTime());
                return "取消预约成功";
            } else {
                return "取消预约失败";
            }
        }
        return "您没有预约记录，请核对预约科室和时间";
    }

    @Tool(name = "查询是否有号源", value = "根据科室名称、日期、时段和可选的医生姓名查询号源情况。" +
            "如果未指定医生姓名，返回该科室该时段所有可预约的医生列表及剩余号数；" +
            "如果指定了医生姓名，返回该医生在该时段的号源情况。")
    public String queryDepartment(
            @P(value = "科室名称") String name,
            @P(value = "日期") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "医生名称", required = false) String doctorName
    ) {
        return doctorScheduleService.queryAvailability(name, date, time, doctorName);
    }
}
