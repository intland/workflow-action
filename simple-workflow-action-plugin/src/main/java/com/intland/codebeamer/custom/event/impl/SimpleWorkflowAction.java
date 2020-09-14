/*
 * Copyright by Intland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */
package com.intland.codebeamer.custom.event.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.intland.codebeamer.event.BaseEvent;
import com.intland.codebeamer.event.impl.AbstractWorkflowActionPlugin;
import com.intland.codebeamer.event.util.VetoException;
import com.intland.codebeamer.manager.util.ActionData;
import com.intland.codebeamer.manager.workflow.ActionCall;
import com.intland.codebeamer.manager.workflow.ActionParam;
import com.intland.codebeamer.manager.workflow.ActionWarning;
import com.intland.codebeamer.manager.workflow.WorkflowAction;
import com.intland.codebeamer.manager.workflow.WorkflowPhase;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;

@Component("simpleWorkflowAction")
@WorkflowAction(value = "simpleWorkflowAction")
public class SimpleWorkflowAction extends AbstractWorkflowActionPlugin {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(SimpleWorkflowAction.class);

	private static final String PARAMETERS = "parameter";

	public SimpleWorkflowAction() {
	}

	@ActionCall(WorkflowPhase.Before)
	public void beforeActionCall(BaseEvent<ArtifactDto, TrackerItemDto, ActionData<?>> event,
			TrackerItemDto trackerItem, @ActionParam(value = PARAMETERS, width = 100) String parameter)
			throws Exception {

		if (StringUtils.isEmpty(parameter)) {
			logger.info("No parameter is passed - Before");
			return;
		}

		if ("warning".equalsIgnoreCase(parameter)) {
			throw new ActionWarning("Warning exception is thrown - Before");
		}

		if ("veto".equalsIgnoreCase(parameter)) {
			throw new VetoException("Veto exception is thrown - Before");
		}

	}

	@ActionCall(WorkflowPhase.After)
	public void afterActionCall(BaseEvent<ArtifactDto, TrackerItemDto, ActionData<?>> event, TrackerItemDto trackerItem,
			@ActionParam(value = PARAMETERS, width = 100) String parameter) throws Exception {

		if (StringUtils.isEmpty(parameter)) {
			logger.info("No parameter is passed - Before");
			return;
		}

		if ("warning".equalsIgnoreCase(parameter)) {
			throw new ActionWarning("Warning exception is thrown - Before");
		}

		if ("veto".equalsIgnoreCase(parameter)) {
			throw new VetoException("Veto exception is thrown - Before");
		}

	}

}
