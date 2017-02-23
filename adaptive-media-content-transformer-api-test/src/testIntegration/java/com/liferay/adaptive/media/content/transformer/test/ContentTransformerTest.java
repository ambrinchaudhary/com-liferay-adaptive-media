/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.adaptive.media.content.transformer.test;

import com.liferay.adaptive.media.AdaptiveMediaException;
import com.liferay.adaptive.media.content.transformer.ContentTransformer;
import com.liferay.adaptive.media.content.transformer.ContentTransformerContentType;
import com.liferay.adaptive.media.content.transformer.ContentTransformerHandler;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.test.rule.ExpectedLog;
import com.liferay.portal.test.rule.ExpectedLogs;
import com.liferay.portal.test.rule.ExpectedType;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.registry.Registry;
import com.liferay.registry.RegistryUtil;
import com.liferay.registry.ServiceRegistration;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.BundleException;

/**
 * @author Alejandro Tardín
 */
@RunWith(Arquillian.class)
@Sync
public class ContentTransformerTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	public void setUp() throws BundleException {
		_contentTransformerHandler = _getService(
			ContentTransformerHandler.class);
	}

	@After
	public void tearDown() throws BundleException {
		_serviceRegistrations.forEach(ServiceRegistration::unregister);
		_serviceRegistrations.clear();
	}

	@Test
	public void testIgnoresTheContentTransformersForDifferentContentTypes()
		throws Exception {

		ContentTransformerContentType<String> contentTypeA =
			new TestContentTransformerContentType<>();
		TestContentTransformerContentType<String> contentTypeB =
			new TestContentTransformerContentType<>();

		String transformedContentA = "transformedContentA";
		String transformedContentB = "transformedContentB";

		_registerContentTransformer(
			new TestContentTransformer(
				contentTypeA, _ORIGINAL_CONTENT, transformedContentA));

		_registerContentTransformer(
			new TestContentTransformer(
				contentTypeB, _ORIGINAL_CONTENT, transformedContentB));

		Assert.assertEquals(
			transformedContentA,
			_contentTransformerHandler.transform(
				contentTypeA, _ORIGINAL_CONTENT));

		Assert.assertEquals(
			transformedContentB,
			_contentTransformerHandler.transform(
				contentTypeB, _ORIGINAL_CONTENT));
	}

	@Test
	public void testReturnsTheContentTransformedByAChainOfContentTransformers()
		throws Exception {

		String intermediateTransformedContent =
			"intermediateTransformedContent";
		String finalTransformedContent = "finalTransformedContent";

		_registerContentTransformer(
			new TestContentTransformer(
				_contentType, _ORIGINAL_CONTENT,
				intermediateTransformedContent));

		_registerContentTransformer(
			new TestContentTransformer(
				_contentType, intermediateTransformedContent,
				finalTransformedContent));

		Assert.assertEquals(
			finalTransformedContent,
			_contentTransformerHandler.transform(
				_contentType, _ORIGINAL_CONTENT));
	}

	@Test
	public void testReturnsTheContentTransformedByATransformerForAContentType()
		throws Exception {

		String transformedContent = "transformedContent";

		_registerContentTransformer(
			new TestContentTransformer(
				_contentType, _ORIGINAL_CONTENT, transformedContent));

		Assert.assertEquals(
			transformedContent,
			_contentTransformerHandler.transform(
				_contentType, _ORIGINAL_CONTENT));
	}

	@ExpectedLogs(
		expectedLogs = {
			@ExpectedLog(
				expectedLog = "Error processing content",
				expectedType = ExpectedType.EXACT
			)
		},
		level = "ERROR", loggerClass = ContentTransformerHandler.class
	)
	@Test
	public void testReturnsTheSameContentIfATransformerThrowsAnException()
		throws Exception {

		_registerContentTransformer(
			new FailingContentTransformer(_contentType, _ORIGINAL_CONTENT));

		Assert.assertSame(
			_ORIGINAL_CONTENT,
			_contentTransformerHandler.transform(
				_contentType, _ORIGINAL_CONTENT));
	}

	@Test
	public void testReturnsTheSameContentIfThereAreNoContentTransformers() {
		Assert.assertSame(
			_ORIGINAL_CONTENT,
			_contentTransformerHandler.transform(
				_contentType, _ORIGINAL_CONTENT));
	}

	@ExpectedLogs(
		expectedLogs = {
			@ExpectedLog(
				expectedLog = "Error processing content",
				expectedType = ExpectedType.EXACT
			)
		},
		level = "ERROR", loggerClass = ContentTransformerHandler.class
	)
	@Test
	public void testRunsTheOtherTransformersEvenIfOneOfThemFails()
		throws Exception {

		String transformedContent = "transformedContent";

		_registerContentTransformer(
			new FailingContentTransformer(_contentType, _ORIGINAL_CONTENT));

		_registerContentTransformer(
			new TestContentTransformer(
				_contentType, _ORIGINAL_CONTENT, transformedContent));

		Assert.assertEquals(
			transformedContent,
			_contentTransformerHandler.transform(
				_contentType, _ORIGINAL_CONTENT));
	}

	private <T> T _getService(Class<T> clazz) {
		Registry registry = RegistryUtil.getRegistry();

		return registry.getService(clazz);
	}

	private void _registerContentTransformer(
		ContentTransformer<String> contentTransformer) {

		Registry registry = RegistryUtil.getRegistry();

		ServiceRegistration<ContentTransformer> serviceRegistration =
			registry.registerService(
				ContentTransformer.class, contentTransformer, null);

		_serviceRegistrations.add(serviceRegistration);
	}

	private static final String _ORIGINAL_CONTENT = "originalContent";

	private ContentTransformerHandler _contentTransformerHandler;
	private final ContentTransformerContentType<String> _contentType =
		new TestContentTransformerContentType<>();
	private final List<ServiceRegistration<ContentTransformer>>
		_serviceRegistrations = new ArrayList<>();

	private static class TestContentTransformerContentType<T>
		implements ContentTransformerContentType<T> {

		@Override
		public String getKey() {
			return "test";
		}

	}

	private class FailingContentTransformer extends TestContentTransformer {

		public FailingContentTransformer(
			ContentTransformerContentType<String> contentType,
			String originalContent) {

			super(contentType, originalContent, null);
		}

		@Override
		public String transform(String content)
			throws AdaptiveMediaException, PortalException {

			if (content.equals(originalContent)) {
				throw new AdaptiveMediaException(
					"Do not worry :), this is an expected exception");
			}

			return null;
		}

	}

	private class TestContentTransformer implements ContentTransformer<String> {

		public TestContentTransformer(
			ContentTransformerContentType<String> contentType,
			String originalContent, String transformedContent) {

			this.contentType = contentType;
			this.originalContent = originalContent;
			this.transformedContent = transformedContent;
		}

		@Override
		public ContentTransformerContentType<String> getContentType() {
			return contentType;
		}

		@Override
		public String transform(String content)
			throws AdaptiveMediaException, PortalException {

			if (content.equals(originalContent)) {
				return transformedContent;
			}

			return null;
		}

		protected ContentTransformerContentType<String> contentType;
		protected String originalContent;
		protected String transformedContent;

	}

}