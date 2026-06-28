package com.huxi.java.ai.langchain4j;

import com.huxi.java.ai.langchain4j.entity.Appointment;
import com.huxi.java.ai.langchain4j.service.AppointmentService;
import com.huxi.java.ai.langchain4j.service.DoctorScheduleService;
import com.huxi.java.ai.langchain4j.tools.AppointmentTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentTool (Function Call) Test")
class AppointmentToolTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private DoctorScheduleService doctorScheduleService;

    @InjectMocks
    private AppointmentTool appointmentTool;

    @BeforeEach
    void setUp() {
        // @InjectMocks handles field injection automatically
    }

    // ==================== queryDepartment ====================

    @Test
    @DisplayName("queryDepartment: 委托 DoctorScheduleService 并返回结果")
    void testQueryDepartmentDelegates() {
        when(doctorScheduleService.queryAvailability("神经内科", "2025-06-25", "上午", null))
                .thenReturn("可预约：王芳（主任医师，剩17个号）、赵强（主治医师，剩5个号）");

        String result = appointmentTool.queryDepartment("神经内科", "2025-06-25", "上午", null);

        assertThat(result).contains("可预约").contains("王芳").contains("赵强");
        verify(doctorScheduleService).queryAvailability("神经内科", "2025-06-25", "上午", null);
    }

    @Test
    @DisplayName("queryDepartment: 指定医生名参数正确传递")
    void testQueryDepartmentWithDoctorName() {
        when(doctorScheduleService.queryAvailability("皮肤科", "2025-06-25", "下午", "陈静"))
                .thenReturn("陈静医生（副主任医师，银屑病）可预约，剩余18个号");

        String result = appointmentTool.queryDepartment("皮肤科", "2025-06-25", "下午", "陈静");

        assertThat(result).contains("陈静").contains("可预约");
        verify(doctorScheduleService).queryAvailability("皮肤科", "2025-06-25", "下午", "陈静");
    }

    // ==================== bookAppointment ====================

    @Test
    @DisplayName("bookAppointment: 新预约成功 + 扣减号源")
    void testBookAppointmentSuccess() {
        Appointment appt = buildAppointment();
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(null);
        when(appointmentService.save(any(Appointment.class))).thenReturn(true);
        when(doctorScheduleService.decrementSlot("王芳", "2025-06-25", "上午")).thenReturn(true);

        String result = appointmentTool.bookAppointment(appt);

        assertThat(result).isEqualTo("预约成功，并返回预约详情");
        verify(appointmentService).save(appt);
        verify(doctorScheduleService).decrementSlot("王芳", "2025-06-25", "上午");
    }

    @Test
    @DisplayName("bookAppointment: 重复预约被拒绝")
    void testBookAppointmentDuplicate() {
        Appointment appt = buildAppointment();
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(appt);

        String result = appointmentTool.bookAppointment(appt);

        assertThat(result).isEqualTo("您在相同的科室和时间已有预约");
        verify(appointmentService, never()).save(any());
        verify(doctorScheduleService, never()).decrementSlot(any(), any(), any());
    }

    @Test
    @DisplayName("bookAppointment: 保存失败时不扣减号源")
    void testBookAppointmentSaveFailed() {
        Appointment appt = buildAppointment();
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(null);
        when(appointmentService.save(any(Appointment.class))).thenReturn(false);

        String result = appointmentTool.bookAppointment(appt);

        assertThat(result).isEqualTo("预约失败");
        verify(doctorScheduleService, never()).decrementSlot(any(), any(), any());
    }

    // ==================== cancelAppointment ====================

    @Test
    @DisplayName("cancelAppointment: 取消成功 + 释放号源")
    void testCancelAppointmentSuccess() {
        Appointment appt = buildAppointment();
        appt.setId(1L);
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(appt);
        when(appointmentService.removeById(1L)).thenReturn(true);

        String result = appointmentTool.cancelAppointment(appt);

        assertThat(result).isEqualTo("取消预约成功");
        verify(doctorScheduleService).incrementSlot("王芳", "2025-06-25", "上午");
    }

    @Test
    @DisplayName("cancelAppointment: 无预约记录时返回提示")
    void testCancelAppointmentNotFound() {
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(null);

        String result = appointmentTool.cancelAppointment(buildAppointment());

        assertThat(result).isEqualTo("您没有预约记录，请核对预约科室和时间");
        verify(doctorScheduleService, never()).incrementSlot(any(), any(), any());
    }

    @Test
    @DisplayName("cancelAppointment: 删除失败时不释放号源")
    void testCancelAppointmentDeleteFailed() {
        Appointment appt = buildAppointment();
        appt.setId(1L);
        when(appointmentService.getOne(any(Appointment.class))).thenReturn(appt);
        when(appointmentService.removeById(1L)).thenReturn(false);

        String result = appointmentTool.cancelAppointment(appt);

        assertThat(result).isEqualTo("取消预约失败");
        verify(doctorScheduleService, never()).incrementSlot(any(), any(), any());
    }

    // ==================== helper ====================

    private Appointment buildAppointment() {
        Appointment appt = new Appointment();
        appt.setUsername("张三");
        appt.setIdCard("123456789012345678");
        appt.setDepartment("神经内科");
        appt.setDate("2025-06-25");
        appt.setTime("上午");
        appt.setDoctorName("王芳");
        return appt;
    }
}
