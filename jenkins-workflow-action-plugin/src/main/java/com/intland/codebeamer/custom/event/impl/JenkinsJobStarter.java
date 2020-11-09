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

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intland.codebeamer.event.BaseEvent;
import com.intland.codebeamer.event.impl.AbstractWorkflowActionPlugin;
import com.intland.codebeamer.manager.util.ActionData;
import com.intland.codebeamer.manager.workflow.ActionCall;
import com.intland.codebeamer.manager.workflow.ActionParam;
import com.intland.codebeamer.manager.workflow.ActionWarning;
import com.intland.codebeamer.manager.workflow.WorkflowAction;
import com.intland.codebeamer.manager.workflow.WorkflowPhase;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;

@Component("jenkinsJobStarter")
@WorkflowAction(value = "jenkinsJobStarter", iconUrl = "/images/Snapshot.png")
public class JenkinsJobStarter extends AbstractWorkflowActionPlugin {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(JenkinsJobStarter.class);

	private static final String BODY_PARAMETER_NAME = "json";

	private static final String JENKINS_JOB_PARAMETERS = "jenkinsJobParameters";

	@Autowired
	private ObjectMapper mapper;

	public JenkinsJobStarter() {
	}

	@ActionCall(WorkflowPhase.After)
	public void startJenkinsJob(BaseEvent<ArtifactDto, TrackerItemDto, ActionData<?>> event, TrackerItemDto trackerItem,
			@ActionParam(value = JENKINS_JOB_PARAMETERS, width = 100) Map<String, Object> jenkinsJobParameters) throws Exception {

		String url = "https://jenkins.com/view/TestAPI/job/Test/build";
		String userName = "<username>";
		String password = "<password>";

		StringEntity entity = buildHttpEntity(jenkinsJobParameters);

		try {
			
			URI uri = URI.create(url);
			HttpHost host = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
			CredentialsProvider credsProvider = createCredentialsProvider(host, userName, password);

			startJenkinsJob(entity, uri, host, credsProvider);
			
		} catch (Exception e) {
			logger.error("Jenkins job cannot be started", e);
			throw new ActionWarning("Jenkins job cannot be started", e);
		}

	}

	private void startJenkinsJob(StringEntity entity, URI uri, HttpHost host, CredentialsProvider credsProvider) throws Exception {
		HttpPost httpRequest = new HttpPost(uri);
		httpRequest.setEntity(entity);

		try (CloseableHttpClient httpClient = createHttpClient(credsProvider)) {

			HttpResponse response = httpClient.execute(host, httpRequest, createLocalContext(host));

			if (!HttpStatus.valueOf(response.getStatusLine().getStatusCode()).is2xxSuccessful()) {
				String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
				throw new IllegalStateException("Jenkins returned with a not 2xx code, response: " + responseBody);
			}

		} finally {
			EntityUtils.consume(httpRequest.getEntity());
		}
	}

	private List<JenkinsParameter> buildParameters(Map<String, Object> jenkinsJobParameters) {
		return jenkinsJobParameters.entrySet().stream()
				.filter(e -> e.getValue() != null)
				.map(e -> new JenkinsParameter(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	private CloseableHttpClient createHttpClient(CredentialsProvider credsProvider) {
		return HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
	}

	private StringEntity buildHttpEntity(Map<String, Object> jenkinsJobParameters) throws JsonProcessingException {
		String parameters = getJsonParameterAsJson(jenkinsJobParameters);
		logger.info("Following parameters will be used: {}", parameters);
		return new StringEntity(String.format("%s=%s", BODY_PARAMETER_NAME, parameters), ContentType.APPLICATION_FORM_URLENCODED);
	}

	private String getJsonParameterAsJson(Map<String, Object> jenkinsJobParameters) throws JsonProcessingException {
		return mapper.writeValueAsString(new JenkinsJobData(getParameters(jenkinsJobParameters)));
	}

	private List<JenkinsParameter> getParameters(Map<String, Object> jenkinsJobParameters) {
		if (MapUtils.isEmpty(jenkinsJobParameters)) {
			return Collections.emptyList();
		}
		
		return buildParameters(jenkinsJobParameters);
	}

	private HttpClientContext createLocalContext(HttpHost host) {
		HttpClientContext localContext = HttpClientContext.create();
		localContext.setAuthCache(creatAuthCache(host));
		return localContext;
	}

	private AuthCache creatAuthCache(HttpHost host) {
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);

		return authCache;
	}

	private static CredentialsProvider createCredentialsProvider(HttpHost httpHost, String userName, String password) {
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(httpHost.getHostName(), httpHost.getPort()), credentials);

		return credsProvider;
	}

	class JenkinsJobData implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private List<JenkinsParameter> parameter;

		public JenkinsJobData() {
			super();
		}

		public JenkinsJobData(List<JenkinsParameter> parameter) {
			super();
			this.parameter = parameter;
		}

		public List<JenkinsParameter> getParameter() {
			return parameter;
		}

		public void setParameter(List<JenkinsParameter> parameter) {
			this.parameter = parameter;
		}

	}

	class JenkinsParameter implements Serializable {

		private static final long serialVersionUID = 1L;

		private String name;

		private String value;

		public JenkinsParameter() {
			super();
		}

		public JenkinsParameter(String name, Object value) {
			super();
			this.name = name;
			this.value = value.toString();
		}

		public JenkinsParameter(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}
}
