package io.github.cocodx.controller;


import org.flowable.engine.*;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author amazfit
 * @date 2022-09-25 下午7:25
 **/
@Controller
@RequestMapping("/expense")
public class ExpenseController {


    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private HistoryService historyService;

    /**
     * 获取流程模型
     * @return
     */
    @RequestMapping("/modelList")
    @ResponseBody
    public List<String> list(){
        List<ProcessDefinition> processList = repositoryService.createProcessDefinitionQuery().list();
        return processList.stream().map(ProcessDefinition::getName).collect(Collectors.toList());
    }



    /**
     * 添加报销
     *
     * @param userId    用户Id
     * @param money     报销金额
     * @param description 描述
     */
    @RequestMapping(value = "add")
    @ResponseBody
    public String addExpense(String userId, Integer money, String description) {
        //启动流程
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskUser", userId);
        map.put("money", money);
        map.put("description", description);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Expense", map);
        return "提交成功.流程Id为：" + processInstance.getId();
    }

    /**
     * 获取审批管理列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list(String userId) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            System.out.println(task.toString());
            System.out.println(task.getId());
        }
        return tasks.toArray().toString();
    }


    /**
     * 批准
     *
     * @param taskId 任务ID
     */
    @RequestMapping(value = "apply")
    @ResponseBody
    public String apply(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("流程不存在");
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "通过");
        taskService.complete(taskId, map);
        return "processed ok!";
    }

    /**
     * 转办任务，把任务转给别人处理【比如说，某个人这几天请假了，他就把任务转给别人处理】
     * @param taskId
     * @return
     */
    @ResponseBody
    @RequestMapping("/transferOther")
    public String transferOther(String taskId,String curUserId,String acceptUserId){
        taskService.setOwner(taskId, curUserId);
        taskService.setAssignee(taskId,acceptUserId);
        return "转办成功";
    }

    /**
     * 委派任务
     * 是将任务节点分给其他人处理，等其他人处理好之后，委派任务会自动回到委派人的任务中
     * @return
     */
    @ResponseBody
    @RequestMapping("/delegateOther")
    public String delegateOther(String taskId,String curUserId,String acceptUserId){
        taskService.setOwner(taskId, curUserId);
        taskService.delegateTask(taskId,acceptUserId);
        return "委派成功";
    }


    /**
     * 拒绝
     */
    @ResponseBody
    @RequestMapping(value = "reject")
    public String reject(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        taskService.complete(taskId, map);
        return "reject";
    }


}
