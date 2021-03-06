package io.openio.sds;

import static io.openio.sds.models.OioUrl.url;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openio.sds.common.SocketProvider;
import io.openio.sds.common.SocketProviders;
import io.openio.sds.exceptions.ContainerNotFoundException;
import io.openio.sds.exceptions.OioException;
import io.openio.sds.exceptions.ReferenceNotFoundException;
import io.openio.sds.http.OioHttp;
import io.openio.sds.http.OioHttpSettings;
import io.openio.sds.models.ContainerInfo;
import io.openio.sds.models.LinkedServiceInfo;
import io.openio.sds.models.ListOptions;
import io.openio.sds.models.NamespaceInfo;
import io.openio.sds.models.ObjectInfo;
import io.openio.sds.models.OioUrl;
import io.openio.sds.models.ServiceInfo;
import io.openio.sds.pool.PoolingSettings;
import io.openio.sds.proxy.ProxyClient;

public class ProxyClientITest {

    private static ProxyClient proxy;

    @BeforeClass
    public static void setup() {
        SocketProvider prov = SocketProviders.pooledSocketProvider(
                new PoolingSettings(), new OioHttpSettings(),
                TestHelper.proxyAddr());
        proxy = new ProxyClient(OioHttp.http(new OioHttpSettings(), prov),
                TestHelper.proxySettings());
    }

    @Test
    public void namespaceInfo() {
        NamespaceInfo ni = proxy.getNamespaceInfo();
        assertNotNull(ni);
        assertNotNull(ni.ns());
        assertNotNull(ni.options());
        assertNotNull(ni.storagePolicies());
    }

    @Test
    public void listServicesWithType() {
        List<ServiceInfo> rawx = proxy.getServices("rawx");
        assertNotNull(rawx);
        for (ServiceInfo si : rawx)
            System.out.println(si);
    }

    @Test(expected = OioException.class)
    public void listServicesWithUnknownType() {
        List<ServiceInfo> rawx = proxy.getServices("unknown");
        assertNotNull(rawx);
        for (ServiceInfo si : rawx)
            System.out.println(si);
    }

    @Test(expected = OioException.class)
    public void listServicesWithNullType() {
        List<ServiceInfo> rawx = proxy.getServices(null);
        assertNotNull(rawx);
        for (ServiceInfo si : rawx)
            System.out.println(si);
    }

    @Test(expected=ContainerNotFoundException.class)
    public void containerNominal() {
        String container = UUID.randomUUID().toString();
        System.out.println(container);
        OioUrl url = OioUrl.url("TEST", container);
        ContainerInfo ci = proxy.createContainer(url);
        assertNotNull(ci);
        assertNotNull(ci.name());
        assertEquals(container, ci.name());
        ci = proxy.getContainerInfo(url);
        System.out.println(proxy.listContainer(url, new ListOptions()));
        System.out.println(ci);
        proxy.deleteContainer(url);
        proxy.getContainerInfo(url);
    }

    @Test(expected = ContainerNotFoundException.class)
    public void getUnknwonContainer() {
        try {
            proxy.getContainerInfo(url("TEST", UUID.randomUUID().toString()));
        } catch (OioException e) {
            try {
                proxy.deleteReference(
                        url("TEST", UUID.randomUUID().toString()));
            } catch (OioException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void getBeansNominal() {
        OioUrl url = url("TEST", UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
        //proxy.createContainer(url);
        try {
            ObjectInfo oinf = proxy.getBeans(url, 1024);
            assertNotNull(oinf);
            assertNotNull(oinf.url());
            assertEquals(url.account(), oinf.url().account());
            assertEquals(url.container(), oinf.url().container());
            assertEquals(url.object(), oinf.url().object());
            Assert.assertNotNull(oinf.oid());
            Assert.assertEquals(1024, oinf.size().longValue());
            Assert.assertTrue(0 < oinf.ctime());
            Assert.assertNotNull(oinf.policy());
            Assert.assertNotNull(oinf.chunkMethod());
            Assert.assertNotNull(oinf.hashMethod());
        } finally {
            proxy.deleteContainer(url);
        }
    }

    @Test(expected = ReferenceNotFoundException.class)
    public void handleReference() {
        OioUrl url = url("TEST", UUID.randomUUID().toString());
        try {
            proxy.createReference(url);
            proxy.showReference(url);
            proxy.deleteReference(url);
        } catch (OioException e) {
            e.printStackTrace();
            Assert.fail();
        }
        proxy.showReference(url);
    }

    @Test
    public void link() {
        OioUrl url = url("TEST", UUID.randomUUID().toString());
        System.out.println(url);
        proxy.createReference(url);
        List<LinkedServiceInfo> l = proxy.linkService(url, "rawx");
        assertNotNull(l);
        assertTrue(0 < l.size());
        List<LinkedServiceInfo> ref = proxy.listServices(url, "rawx");
        assertNotNull(ref);
        assertTrue(0 < ref.size());
    }
}
