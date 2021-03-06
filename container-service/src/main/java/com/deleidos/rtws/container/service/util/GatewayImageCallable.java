package com.deleidos.rtws.container.service.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deleidos.rtws.container.service.model.ImageBuildRequest;
import com.deleidos.rtws.container.service.model.PlaybookParams;

public class GatewayImageCallable extends GenericSystemBaseImageDependentCallable {

	private static class GatewayImageSetupUtil implements BuildAreaSetupUtil {

		private Logger logger = LoggerFactory.getLogger(GatewayImageSetupUtil.class);

		@Override
		public File setupBuildTarget(ImageBuildRequest buildRequest) {
			File target = new File(String.format("%s/%s", buildRequest.getBuildArea().getAbsolutePath(), "gateway"));
			if (!target.exists())
				target.mkdir();

			InputStream is = null;
			try {
				is = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream("dockerfiles/Dockerfile_centos_system_gateway");

				// Copy the Dockerfile and update the base image reference
				File dockerfile = new File(target.getAbsolutePath() + File.separatorChar + "Dockerfile");
				DockerfileUtil.updateFromDeclaration(buildRequest.getDomain(), buildRequest.getSoftwareVersion(), is,
						dockerfile);

				// Copy the playbook
				Path playbook = FileSystems.getDefault().getPath("/tmp/gateway.tar.gz");
				Files.copy(playbook, FileSystems.getDefault()
						.getPath(target.getAbsolutePath() + File.separatorChar + "gateway.tar.gz"));

				// Write the params
				PlaybookParams params = new PlaybookParams(buildRequest.getSoftwareVersion(), buildRequest.getDomain());
				YamlMapperCache.mapper
						.writeValue(new File(target.getAbsolutePath() + File.separatorChar + "params.yml"), params);

			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}

			}

			return target;
		}
	}

	public GatewayImageCallable(ImageBuildRequest buildRequest) {
		super(buildRequest, new GatewayImageSetupUtil(), String.format("%s/%s:%s_%s", "digitaledge", buildRequest.getImageName(),
				"gateway", buildRequest.getSoftwareVersion()));

	}
}