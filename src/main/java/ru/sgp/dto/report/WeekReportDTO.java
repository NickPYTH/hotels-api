package ru.sgp.dto.report;

import lombok.Data;

@Data
public class WeekReportDTO {
    private Integer number;
    private String flatName;
    private String guestListMonday;
    private String reasonListMonday;
    private String guestListTuesday;
    private String reasonListTuesday;
    private String guestListWednesday;
    private String reasonListWednesday;
    private String guestListThursday;
    private String reasonListThursday;
    private String guestListFriday;
    private String reasonListFriday;
    private String guestListSaturday;
    private String reasonListSaturday;
    private String guestListSunday;
    private String reasonListSunday;
    private Integer summary;
}
