package io.github.cocodx.controller;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 流程定义 相关api接口
 * @author amazfit
 * @date 2022-09-25 下午8:51
 **/
@RestController
@RequestMapping("/process")
public class ProcessController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ModelService modelService;

    /**
     * 获取流程定义列表
     * @return
     */
    @RequestMapping("/list")
    public List<ProcessDefinition> list(){
        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
                .list();
        return list;
    }

    /**
     * 流程部署
     * @param file
     * @return
     */
    @RequestMapping("/deploy")
    public Deployment deploy(MultipartFile file,String deployName) throws IOException {
        //完成流程的部署操作
        Deployment deploy = repositoryService.createDeployment()
                .addInputStream(file.getName(),file.getInputStream())
                .name(deployName)
                .deploy()//流程部署
                ;
        return deploy;
    }

    /**
     * 删除流程
     * @param deloyId
     * @return
     */
    @RequestMapping("/deleteProcess")
    public String deleteProcess(String deloyId){
        repositoryService.deleteDeployment(deloyId,true);
        return "删除成功";
    }

    /**
     * 流程状态变更
     * @param processDefinitionId
     * @param status
     * @return
     */
    @RequestMapping("/updateProcessStatus")
    public String suspendProcess(String processDefinitionId,String status){
        if (status.equals("active")){
            repositoryService.activateProcessDefinitionById(processDefinitionId,true,null);
        }else if (status.equals("suspend")){
            repositoryService.suspendProcessDefinitionById(processDefinitionId,true,null);
        }else {
            System.out.println("暂无流程变更");
        }
        return "成功";
    }

    /**
     * 查看xml代码
     * @param processDefinitionId
     * @param response
     */
    @RequestMapping("/xml")
    public void xml(String processDefinitionId,HttpServletResponse response){
        try {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
//            Model model = modelService.getModel(modelId);
            byte[] b = modelService.getBpmnXML(bpmnModel);
            response.setHeader("Content-type", "text/xml;charset=UTF-8");
            response.getOutputStream().write(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成流程图
     *
     * @param processId 任务ID
     */
    @RequestMapping(value = "processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        //1.获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        String processDefinitionId;
        List<String> activeActivityIds = new ArrayList<>();
        List<String> highLightedFlows = new ArrayList<>();
        // 2.获取所有的历史轨迹线对象
        List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(processId)
                .activityType(BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW).list();
        list.forEach(item -> highLightedFlows.add(item.getActivityId()));
        // 3.获取流程定义id和高亮的节点id
        if (processInstance!=null){
            processDefinitionId = processInstance.getProcessDefinitionId();
            activeActivityIds = processEngine.getRuntimeService().getActiveActivityIds(processId);
        }else{
            // 3.2 已经结束的流程实例
            HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceId(processId).singleResult();
            processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            // 3.3 获取结束节点列表
            List<HistoricActivityInstance> historicEnds = processEngine.getHistoryService().createHistoricActivityInstanceQuery()
                    .processInstanceId(processId).activityType(BpmnXMLConstants.ELEMENT_EVENT_END).list();
            List<String> finalActiveActivityIds = activeActivityIds;
            historicEnds.forEach(historicActivityInstance -> finalActiveActivityIds.add(historicActivityInstance.getActivityId()));
        }
        // 4. 获取bpmnModel对象
        BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(processDefinitionId);
        // 5. 生成图片流
        ProcessEngineConfiguration configuration = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = configuration.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activeActivityIds,
                highLightedFlows, "宋体", "宋体", "宋体",
                this.getClass().getClassLoader(), 1.0, true);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * 获取流程定义图片
     * @param httpServletResponse
     * @param processDefinitionId 流程定义id
     * @throws Exception
     */
    @RequestMapping(value = "processModelDiagram")
    public void genProcessModelDiagram(HttpServletResponse httpServletResponse, String processDefinitionId) throws Exception {
//        //获取bpmnModel对象
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        //生成图片流
        ProcessDiagramGenerator diagramGenerator = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel,"png", Collections.emptyList(),Collections.emptyList(), "宋体", "宋体", "宋体",
                this.getClass().getClassLoader(), 1.0, true);
        byte[] buf = new byte[1024];
        int legth = 0;
        OutputStream out = null;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
