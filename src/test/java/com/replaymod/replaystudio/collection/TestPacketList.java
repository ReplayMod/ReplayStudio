/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.collection;


import com.google.common.collect.testing.*;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.testers.*;
import com.replaymod.replaystudio.PacketData;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.packetlib.packet.Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestPacketList.GuavaTests.class,
})
public class TestPacketList extends TestCase {

    public static class GuavaTests {
        private static class Packets extends SampleElements<PacketData> {
            private static final Packet DUMMY_PACKET = new ServerChatPacket("");
            public Packets() {
                super(new PacketData(0, DUMMY_PACKET),
                        new PacketData(1, DUMMY_PACKET),
                        new PacketData(2, DUMMY_PACKET),
                        new PacketData(3, DUMMY_PACKET),
                        new PacketData(4, DUMMY_PACKET));
            }
        }

        private static class PacketListTestSuiteBuilder extends AbstractCollectionTestSuiteBuilder<PacketListTestSuiteBuilder, PacketData> {
            public static PacketListTestSuiteBuilder using(TestListGenerator<PacketData> generator) {
                return new PacketListTestSuiteBuilder().usingGenerator(generator);
            }

            @Override
            protected List<Class<? extends AbstractTester>> getTesters() {
                List<Class<? extends AbstractTester>> testers = Helpers.copyToList(super.getTesters());
                testers.add(CollectionSerializationEqualTester.class);
                testers.add(ListAddAllAtIndexTester.class);
                testers.add(ListAddAtIndexTester.class);
                testers.add(ListCreationTester.class);
                testers.add(ListEqualsTester.class);
                testers.add(ListGetTester.class);
                testers.add(ListHashCodeTester.class);
                testers.add(ListIndexOfTester.class);
                testers.add(ListLastIndexOfTester.class);
                testers.add(ListListIteratorTester.class);
                testers.add(ListRemoveAllTester.class);
                testers.add(ListRemoveAtIndexTester.class);
                testers.add(ListRemoveTester.class);
                testers.add(ListRetainAllTester.class);
                testers.add(ListToArrayTester.class);
                return testers;
            }
        }

        public static TestSuite suite() {
            return PacketListTestSuiteBuilder
                    .using(new TestListGenerator<PacketData>() {
                        @Override
                        public List<PacketData> create(Object... elements) {
                            PacketData[] array = new PacketData[elements.length];
                            int i = 0;
                            for (Object e : elements) {
                                array[i++] = (PacketData) e;
                            }
                            return new PacketList(Arrays.asList(array));
                        }

                        @Override
                        public SampleElements<PacketData> samples() {
                            return new Packets();
                        }

                        @Override
                        public PacketData[] createArray(int length) {
                            return new PacketData[length];
                        }

                        @Override
                        public Iterable<PacketData> order(List<PacketData> insertionOrder) {
                            List<PacketData> sorted = new ArrayList<>(insertionOrder);
                            Collections.sort(sorted, (e1, e2) -> Long.compare(e1.getTime(), e2.getTime()));
                            return sorted;
                        }
                    })
                    .named("PacketList test")
                    .withFeatures(
                            ListFeature.REMOVE_OPERATIONS,
                            ListFeature.SUPPORTS_REMOVE_WITH_INDEX,
                            ListFeature.SUPPORTS_SET,
                            CollectionFeature.GENERAL_PURPOSE,
                            CollectionFeature.ALLOWS_NULL_QUERIES,
                            CollectionSize.ANY
                    ).createTestSuite();
        }


    }

}
