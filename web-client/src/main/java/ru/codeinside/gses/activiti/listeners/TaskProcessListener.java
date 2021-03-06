/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.activiti.listeners;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import ru.codeinside.adm.AdminServiceProvider;
import ru.codeinside.adm.database.Bid;
import ru.codeinside.adm.database.TaskDates;
import ru.codeinside.gses.activiti.Activiti;
import ru.codeinside.gses.activiti.forms.CustomTaskFormHandler;
import ru.codeinside.gses.activiti.forms.api.duration.DurationPreference;
import ru.codeinside.gses.activiti.forms.api.duration.LazyCalendar;
import ru.codeinside.gses.webui.Flash;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

public class TaskProcessListener implements TaskListener {
  @Override
  public void notify(final DelegateTask execution) {
    final Event event = Event.valueOf(execution);
    Bid firstBid = null;
    EntityManager em = Activiti.getEm();
    List<Bid> resultList = em
      .createQuery("select e from Bid e where e.processInstanceId = :processInstanceId", Bid.class)
      .setParameter("processInstanceId", execution.getProcessInstanceId()).getResultList();
    for (final Bid bid : resultList) {
      if (firstBid == null) {
        firstBid = bid;
      }
      if (event == Event.Create) {
        bid.getCurrentSteps().add(execution.getId());
        // событие назначения может быть ДО события создания!
        TaskDates task = em.find(TaskDates.class, execution.getId());
        if (task == null) {
          task = createTaskDates(execution, firstBid);
          em.persist(task);
          em.flush();
        }
      } else if (event == Event.Complete) {
        bid.getCurrentSteps().remove(execution.getId());
      }
    }

    String info = null;
    String action = null;
    if (event == Event.Complete) {
      if (firstBid != null && firstBid.getProcedure() != null) {
        info = "procedureId: " + firstBid.getProcedure().getId();
      }
      action = "complete";
    } else if (event == Event.Assignment) {

      // фиксировать дату назначания только при реальном назначении
      if (execution.getAssignee() != null) {
        TaskDates task = em.find(TaskDates.class, execution.getId());
        boolean needFlush = false;
        if (task == null) {
          task = createTaskDates(execution, firstBid);
          needFlush = true;
        }
        if (task.getAssignDate() == null) {
          task.setAssignDate(new Date());
          needFlush = true;
        }
        if (needFlush) {
          em.persist(task);
          em.flush();
        }
      }

      info = "assigned: " + execution.getAssignee();
      if (firstBid != null && firstBid.getProcedure() != null) {
        info += ", procedureId: " + firstBid.getProcedure().getId();
      }
      action = "assign";
    }
    if (event == Event.Assignment || event == Event.Complete) {
      AdminServiceProvider.get().createLog(Flash.getActor(), "task", execution.getId(), action, info, true);
    }
    if (event == Event.Complete) {
      em.createQuery("delete from FormBuffer where taskId=:id")
        .setParameter("id", execution.getId())
        .executeUpdate(); // каскадное удаления для связных объектов
    }
  }

  private TaskDates createTaskDates(DelegateTask execution, Bid bid) {
    TaskDates taskDates = new TaskDates();
    taskDates.setId(execution.getId());
    taskDates.setBid(bid);
    taskDates.setStartDate(execution.getCreateTime());
    getDurationPreference(execution).initializeTaskDates(taskDates, new LazyCalendar());
    return taskDates;
  }

  private DurationPreference getDurationPreference(DelegateTask execution) {
    return ((CustomTaskFormHandler) ((TaskEntity) execution).getTaskDefinition().getTaskFormHandler())
      .getPropertyTree()
      .getDurationPreference();
  }
}