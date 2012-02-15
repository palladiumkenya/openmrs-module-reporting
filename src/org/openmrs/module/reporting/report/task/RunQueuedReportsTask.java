package org.openmrs.module.reporting.report.task;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.ReportingConstants;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.ReportRequest.PriorityComparator;
import org.openmrs.module.reporting.report.ReportRequest.Status;
import org.openmrs.module.reporting.report.service.ReportService;

/**
 * If there are any queued reports to run, this task starts running the next one.
 */
public class RunQueuedReportsTask extends AbstractReportsTask {
	
	private static Log log = LogFactory.getLog(RunQueuedReportsTask.class);
	
	private static Integer maxExecutions = null;
	private static Set<ReportRequest> currentlyRunningRequests = new HashSet<ReportRequest>();
	
	/**
	 * @see AbstractReportsTask#execute()
	 */
	@Override
	public synchronized void execute() {
		
		if (maxExecutions == null) {
			maxExecutions = ReportingConstants.GLOBAL_PROPERTY_MAX_REPORTS_TO_RUN();
		}
		
		log.debug("Executing the Run Queued Reports Task");
		
		ReportService rs = Context.getService(ReportService.class);
		
		if (currentlyRunningRequests.size() < maxExecutions) {
			List<ReportRequest> l = rs.getReportRequests(null, null, null, Status.REQUESTED);
			if (!l.isEmpty()) {
				Collections.sort(l, new PriorityComparator());
				for (int i=1; i<l.size(); i++) {
					rs.logReportMessage(l.get(i), "Report in queue at position " + i);
				}
				ReportRequest requestToRun = l.get(0);
		    	ParameterizableUtil.refreshMappedDefinition(requestToRun.getReportDefinition());
		    	if (requestToRun.getBaseCohort() != null) {
		    		ParameterizableUtil.refreshMappedDefinition(requestToRun.getBaseCohort());
		    	}
		    	if (!currentlyRunningRequests.contains(requestToRun)) {
		    		try {
		    			currentlyRunningRequests.add(requestToRun);
			    		rs.runReport(requestToRun);
			    	}
		    		finally {
		    			currentlyRunningRequests.remove(requestToRun);
		    		}
				}
			}
		}
	}
}