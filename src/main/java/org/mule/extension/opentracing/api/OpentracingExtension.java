package org.mule.extension.opentracing.api;

import static org.mule.runtime.api.meta.Category.COMMUNITY;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.extension.opentracing.internal.*;

/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "opentracing")
@Extension(name = "Opentracing", vendor="Gaston Panizza", category = COMMUNITY)
@ConnectionProviders(OpentracingConnectionProvider.class)
@Operations({OpentracingOperations.class})
public class OpentracingExtension {

}
