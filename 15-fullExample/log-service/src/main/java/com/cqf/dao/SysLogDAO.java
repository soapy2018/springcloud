package com.cqf.dao;

import com.cqf.entity.SysLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by cqf on 2019/10/9
 */
public interface SysLogDAO extends JpaRepository<SysLog, Long> {
}
