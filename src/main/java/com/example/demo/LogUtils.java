package com.example.demo;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.Builder;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactory;
import org.springframework.core.env.Environment;

import lombok.NonNull;

public class LogUtils {

	// Constants
	public static String KEYWORD_PATTERN_LAYOUT = "pattern";
	public static String KEYWORD_JSON_TEMPLATE_LAYOUT = "json-template";
	
	// logger
	private static Logger logger = null;
	
	// root
	private static boolean logRootEnable = true;
	private static Level logRootLevel = Level.INFO;
	
	// console
	private static boolean logConsoleEnable = true;
	private static String logConsolePattern = KEYWORD_PATTERN_LAYOUT;
	
	// file
	private static boolean logFileEnable = false;
	private static String logFilePattern = KEYWORD_PATTERN_LAYOUT;
	
	
	public static void setLog(Environment env) {
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		Configuration loggerConfig = loggerContext.getConfiguration();
		setAppenderLog(loggerConfig, env);
		loggerContext.updateLoggers();
		LogUtils.logger = LogManager.getLogger(LogUtils.class.getName());
	}
	
	private static void setAppenderLog(Configuration loggerConfig, Environment env) {
		Boolean logConsoleEnableTemp = convertStrToBoolean(env.getProperty("custom.log.console.enable"));
		String logConsolePatternTemp = checkLayout(env.getProperty("custom.log.console.layout"));
		if (null != logConsoleEnableTemp) LogUtils.logConsoleEnable = logConsoleEnableTemp;
		if (null != logConsolePatternTemp) LogUtils.logConsolePattern = logConsolePatternTemp;
		
		Boolean logFileEnableTemp = convertStrToBoolean(env.getProperty("custom.log.file.enable"));
		String logFilePatternTemp = checkLayout(env.getProperty("custom.log.file.layout"));
		if (null != logFileEnableTemp) LogUtils.logFileEnable = logFileEnableTemp;
		if (null != logFilePatternTemp) LogUtils.logFilePattern = logFilePatternTemp;
		
		Appender appenderConsole = null;
		Appender appenderLogFile = null;
		if (LogUtils.logConsoleEnable) {
			Layout<? extends Serializable> layout = null;
			if (LogUtils.logConsolePattern.equals(LogUtils.KEYWORD_PATTERN_LAYOUT)) {
				layout = createPatternLayout(loggerConfig, null);
			} else {
				layout = createJsonTemplateLayout(loggerConfig, null, null);
			}
			appenderConsole = createConsoleAppender(loggerConfig, "ConsoleAppender", layout);
			appenderConsole.start();
		}
		if (LogUtils.logFileEnable) {
			// TODO 
		}
		
		AppenderRef[] refs = createAppenderRefArray();
		configLogRoot(loggerConfig, env, refs, appenderConsole);
	}
	
	
	private static void configLogRoot(Configuration loggerConfig, Environment env, AppenderRef[] refs, Appender appender) {
		Boolean logRootEnableTemp = convertStrToBoolean(env.getProperty("custom.log.root.enable"));
		String levelTemp = env.getProperty("custom.log.root.level");
		Level logRootLevelTemp = null;
		if (StringUtils.isNotBlank(levelTemp)) logRootLevelTemp = Level.getLevel(levelTemp); 
		if (null != logRootEnableTemp) LogUtils.logRootEnable = logRootEnableTemp;
		if (null != logRootLevelTemp) LogUtils.logRootLevel = logRootLevelTemp;
		
		Map<String, LoggerConfig> loggerConfigMap = loggerConfig.getLoggers();
		LoggerConfig loggerConfigRoot = loggerConfigMap.get("");
		if (null != loggerConfigRoot) {
			loggerConfig.removeLogger("");
			loggerConfigRoot = null;
		}
		if (LogUtils.logRootEnable) {
			loggerConfigRoot = LoggerConfig.createLogger(true, LogUtils.logRootLevel, "root", "true", refs, null, loggerConfig, null);
			loggerConfigRoot.addAppender(appender, null, null);
			loggerConfig.addLogger("", loggerConfigRoot);
		}
		
	}
	
	private static AppenderRef[] createAppenderRefArray() {
		AppenderRef consoleAppenderref = AppenderRef.createAppenderRef("ConsoleAppender", null, null);
		AppenderRef[] refs = new AppenderRef[] { consoleAppenderref };
		return refs;
	}
	
	
	/*
	 * CONFIG METHOD
	 */
	private static String checkLayout(String value) {
		if (LogUtils.KEYWORD_JSON_TEMPLATE_LAYOUT.equalsIgnoreCase(value)) return LogUtils.KEYWORD_JSON_TEMPLATE_LAYOUT;
		else if (LogUtils.KEYWORD_PATTERN_LAYOUT.equalsIgnoreCase(value)) return LogUtils.KEYWORD_PATTERN_LAYOUT;
		return null;
	}
	
	private static Boolean convertStrToBoolean(String value) {
		if ("true".equalsIgnoreCase(value)) return true;
		else if ("false".equalsIgnoreCase(value)) return false;
		return null;
	}
	
	private static String checkPrefix(String prefix) {
		return StringUtils.defaultString(prefix);
	}
	
	private static Appender createConsoleAppender(Configuration loggerConfig, String appenderName, Layout<? extends Serializable> layout) {
		return ConsoleAppender.newBuilder()
				.setConfiguration(loggerConfig)
				.setName(appenderName)
				.setLayout(layout)
				.setTarget(Target.SYSTEM_OUT)
				.build();
	}
	
	private static JsonTemplateLayout createJsonTemplateLayout(Configuration loggerConfig, String eventTemplateUri, Map<String, String> addField) {
		RecyclerFactory recyclerFactory = JsonTemplateLayoutDefaults.getRecyclerFactory();
		Builder builder =  JsonTemplateLayout.newBuilder()
	            .setConfiguration(loggerConfig)
	            .setCharset(StandardCharsets.UTF_8)
	            .setRecyclerFactory(recyclerFactory);
		if (StringUtils.isNotBlank(eventTemplateUri)) {
			builder.setEventTemplateUri(eventTemplateUri);
		} else if (null != addField && !addField.isEmpty()) {
			builder.setEventTemplateUri("classpath:log-template/GelfLayout.json");
		} else {
			builder.setEventTemplateUri("classpath:log-template/LogstashJsonEventLayoutV1.json");
		}
		
		if (null != addField && !addField.isEmpty()) {
			List<EventTemplateAdditionalField> arrayAddField = new ArrayList<>();
			addField.entrySet().stream().forEach(entrySet -> {
				arrayAddField.add(EventTemplateAdditionalField.newBuilder().setKey(entrySet.getKey()).setValue(entrySet.getValue()).build());
			});
			builder.setEventTemplateAdditionalFields(arrayAddField.toArray(new EventTemplateAdditionalField[arrayAddField.size()]));
		}
		
		return builder.build();
	}
	
	private static PatternLayout createPatternLayout(Configuration loggerConfig, String pattern) {
		if (StringUtils.isBlank(pattern)) pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] [%level] %logger{36} - %msg%n";
		return PatternLayout.newBuilder()
				  .withConfiguration(loggerConfig)
				  .withPattern(pattern)
				  .withCharset(StandardCharsets.UTF_8)
				  .build();
	}
	
	/*
	 * PUBLIC METHOD
	 */
	
	public static String stackTraceToString(@NonNull Throwable t, @NonNull String prefix) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		StringBuffer stb = writer.getBuffer();
		stb.deleteCharAt(stb.length() - 1);
		return stb.toString().replace("\n", "\n" + prefix);
	}
	
	public static String getThreadContext(@NonNull String key) {
		return ThreadContext.get(key);
	}
	
	public static void setThreadContext(@NonNull String key, @NonNull String value) {
		ThreadContext.put(key, value);
	}
	
	public static void clearThreadContext() {
		ThreadContext.clearAll();
	}

	public static void debug(@NonNull String message) {
		debug(message, null, null);
	}
	
	public static void debug(@NonNull Throwable error, @NonNull String prefix) {
		debug(null, error, prefix);
	}

	public static void debug(String message, Throwable error, String prefix) {
		StringBuilder str = StringUtils.isNotBlank(message) ?  new StringBuilder(message).append(" ") : new StringBuilder();
		if (null != error) {
			prefix = checkPrefix(prefix);
			str.append(stackTraceToString(error, prefix));
		}
		LogUtils.logger.debug(str.toString());
	}
	
	public static void info(@NonNull String message) {
		info(message, null, null);
	}
	
	public static void info(@NonNull Throwable error, @NonNull String prefix) {
		info(null, error, prefix);
	}

	public static void info(String message, Throwable error, String prefix) {
		StringBuilder str = StringUtils.isNotBlank(message) ?  new StringBuilder(message).append(" ") : new StringBuilder();
		if (null != error) {
			prefix = checkPrefix(prefix);
			str.append(stackTraceToString(error, prefix));
		}
		LogUtils.logger.info(str.toString());
	}
	
	public static void warn(@NonNull String message) {
		warn(message, null, null);
	}
	
	public static void warn(@NonNull Throwable error, @NonNull String prefix) {
		warn(null, error, prefix);
	}

	public static void warn(String message, Throwable error, String prefix) {
		StringBuilder str = StringUtils.isNotBlank(message) ?  new StringBuilder(message).append(" ") : new StringBuilder();
		if (null != error) {
			prefix = checkPrefix(prefix);
			str.append(stackTraceToString(error, prefix));
		}
		LogUtils.logger.warn(str.toString());
	}
	
	public static void error(@NonNull String message) {
		error(message, null, null);
	}
	
	public static void error(@NonNull Throwable error, @NonNull String prefix) {
		error(null, error, prefix);
	}

	public static void error(String message, Throwable error, String prefix) {
		StringBuilder str = StringUtils.isNotBlank(message) ?  new StringBuilder(message).append(" ") : new StringBuilder();
		if (null != error) {
			prefix = checkPrefix(prefix);
			str.append(stackTraceToString(error, prefix));
		}
		LogUtils.logger.error(str.toString());
	}
	
	public static void fatal(@NonNull String message) {
		fatal(message, null, null);
	}
	
	public static void fatal(@NonNull Throwable error, @NonNull String prefix) {
		fatal(null, error, prefix);
	}

	public static void fatal(String message, Throwable error, String prefix) {
		StringBuilder str = StringUtils.isNotBlank(message) ?  new StringBuilder(message).append(" ") : new StringBuilder();
		if (null != error) {
			prefix = checkPrefix(prefix);
			str.append(stackTraceToString(error, prefix));
		}
		LogUtils.logger.fatal(str.toString());
	}

}
