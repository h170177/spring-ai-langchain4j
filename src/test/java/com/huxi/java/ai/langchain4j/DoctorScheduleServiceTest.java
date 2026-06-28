package com.huxi.java.ai.langchain4j;

import com.huxi.java.ai.langchain4j.entity.Doctor;
import com.huxi.java.ai.langchain4j.entity.DoctorSchedule;
import com.huxi.java.ai.langchain4j.mapper.DoctorMapper;
import com.huxi.java.ai.langchain4j.mapper.DoctorScheduleMapper;
import com.huxi.java.ai.langchain4j.service.Impl.DoctorScheduleServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorScheduleService Unit Test")
class DoctorScheduleServiceTest {

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private DoctorScheduleMapper doctorScheduleMapper;

    @InjectMocks
    private DoctorScheduleServiceImpl doctorScheduleService;

    private Doctor doctorWang;
    private Doctor doctorZhao;
    private DoctorSchedule wangMorningSchedule;

    @BeforeEach
    void setUp() {
        doctorWang = new Doctor(1L, "王芳", "神经内科", "主任医师", "脑血管疾病");
        doctorZhao = new Doctor(2L, "赵强", "神经内科", "主治医师", "帕金森病");

        wangMorningSchedule = new DoctorSchedule(10L, 1L, "2025-06-25",
                "上午", 20, 3); // 已约3人，剩17个号
    }

    // ==================== queryAvailability ====================

    @Test
    @DisplayName("指定医生查询: 有号返回剩余数量")
    void testQueryByDoctorHasSlots() {
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(doctorWang);
        when(doctorScheduleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(wangMorningSchedule);

        String result = doctorScheduleService.queryAvailability(
                "神经内科", "2025-06-25", "上午", "王芳");

        assertThat(result).contains("王芳医生").contains("主任医师").contains("剩余17个号");
    }

    @Test
    @DisplayName("指定医生查询: 号源已满")
    void testQueryByDoctorFull() {
        DoctorSchedule fullSchedule = new DoctorSchedule(10L, 1L, "2025-06-25",
                "上午", 20, 20);
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(doctorWang);
        when(doctorScheduleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(fullSchedule);

        String result = doctorScheduleService.queryAvailability(
                "神经内科", "2025-06-25", "上午", "王芳");

        assertThat(result).contains("号源已约满");
    }

    @Test
    @DisplayName("指定医生查询: 医生不属于该科室")
    void testQueryByDoctorWrongDept() {
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        String result = doctorScheduleService.queryAvailability(
                "心内科", "2025-06-25", "上午", "王芳");

        assertThat(result).contains("心内科没有叫王芳的医生");
    }

    @Test
    @DisplayName("指定医生查询: 当天不值班")
    void testQueryByDoctorNotOnDuty() {
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(doctorWang);
        when(doctorScheduleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        String result = doctorScheduleService.queryAvailability(
                "神经内科", "2025-06-25", "上午", "王芳");

        assertThat(result).contains("不值班");
    }

    @Test
    @DisplayName("按科室查询: 返回所有有号医生")
    void testQueryByDepartmentHasAvailable() {
        DoctorSchedule zhaoSchedule = new DoctorSchedule(11L, 2L, "2025-06-25",
                "上午", 20, 15); // 剩5个
        when(doctorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(doctorWang, doctorZhao));
        when(doctorScheduleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(wangMorningSchedule)   // 第一次查王芳
                .thenReturn(zhaoSchedule);          // 第二次查赵强

        String result = doctorScheduleService.queryAvailability(
                "神经内科", "2025-06-25", "上午", null);

        assertThat(result).contains("可预约")
                .contains("王芳").contains("剩17个号")
                .contains("赵强").contains("剩5个号");
    }

    @Test
    @DisplayName("按科室查询: 该科室无医生")
    void testQueryByDepartmentNoDoctors() {
        when(doctorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of());

        String result = doctorScheduleService.queryAvailability(
                "不存在科室", "2025-06-25", "上午", null);

        assertThat(result).contains("暂无医生信息");
    }

    // ==================== decrementSlot ====================

    @Test
    @DisplayName("扣减号源: 正常扣减成功")
    void testDecrementSlotSuccess() {
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(doctorWang);
        when(doctorScheduleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(wangMorningSchedule);

        boolean result = doctorScheduleService.decrementSlot("王芳", "2025-06-25", "上午");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("扣减号源: 号源已满无法扣减")
    void testDecrementSlotWhenFull() {
        DoctorSchedule fullSchedule = new DoctorSchedule(10L, 1L, "2025-06-25",
                "上午", 20, 20);
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(doctorWang);
        when(doctorScheduleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(fullSchedule);

        boolean result = doctorScheduleService.decrementSlot("王芳", "2025-06-25", "上午");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("扣减号源: 医生不存在")
    void testDecrementSlotDoctorNotFound() {
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        boolean result = doctorScheduleService.decrementSlot("不存在", "2025-06-25", "上午");

        assertThat(result).isFalse();
    }

    // ==================== incrementSlot ====================

    @Test
    @DisplayName("释放号源: 正常执行不抛异常")
    void testIncrementSlotSuccess() {
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(doctorWang);

        // incrementSlot 使用 update 语句，无返回值，只需验证不抛异常
        doctorScheduleService.incrementSlot("王芳", "2025-06-25", "上午");
        // no exception = pass
    }

    @Test
    @DisplayName("释放号源: 医生不存在时静默跳过")
    void testIncrementSlotDoctorNotFound() {
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        doctorScheduleService.incrementSlot("不存在", "2025-06-25", "上午");
        // no exception = pass
    }
}
