package org.krocodl.demo.imapfollowupservice.common.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * introduced, to be able to test service
 */
@Service
public class DateService {

    @Value("${dateService.daysOffset}")
    private int daysOffset;

    public Date nowDate() {
        return nowCalendar().getTime();
    }

    public Calendar nowCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset);
        return calendar;
    }

    public void setDaysOffset(final int daysOffset) {
        this.daysOffset = daysOffset;
    }
}
