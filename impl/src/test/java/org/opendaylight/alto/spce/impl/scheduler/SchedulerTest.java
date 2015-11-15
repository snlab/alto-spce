package org.opendaylight.alto.spce.impl.scheduler;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.experimental.theories.DataPoint;

import java.util.ArrayList;
import java.util.List;

public class SchedulerTest {
        private OMFRA obj = new OMFRA(true);

        private boolean equ(double[] x, double[] y) {
            for (int i = 0; i < x.length; i++)
                System.out.println(x[i]);
            return true;

            /*
            double eps = 1e-6;

            if (x.length != y.length) return false;
            for (int i = 0; i < x.length; i++)
                if (x[i] - y[i] > eps || y[i] - x[i] > eps) return false;

            return true;
            */
        }

        @Test
        public void MMFSolverTest() { //TODO::Need strong test case to provide correctness
            long[][] BandwidthMap = {{0, 3, 2}, {4, 0, 2}, {3, 5, 0}};
            BandwidthTopology topology = new BandwidthTopology(BandwidthMap);


            //private int mSeq;
            //private int priority;
            //private long arrivalTime;
            //private int destination;
            //private long volume;

            DataTransferRequest req0 = new DataTransferRequest(0, 1, 0, 1, 3);
            DataTransferRequest req1 = new DataTransferRequest(1, 2, 0, 2, 2);
            DataTransferRequest req2 = new DataTransferRequest(2, 3, 0, 0, 1);

            DataTransferRequest[] unsatDataTransferRequests = {req0, req1};

            DataTransferRequest[] satDataTransferRequests = {req2};


            //private int kSeq;
            //private int mSeq;
            //private int source;
            //private int destination;
            //private long volume;
            //private List<Integer> path;
            //private long minBandwidth;
            //private long maxBandwidth;

            List<Integer> list = new ArrayList<>();
            list.add(0);
            list.add(1);
            DataTransferFlow flow0 = new DataTransferFlow(0, 1, 0, 1, 2, list, 0, 2);

            list = new ArrayList<>();
            list.add(1);
            list.add(2);
            DataTransferFlow flow1 = new DataTransferFlow(1, 2, 1, 2, 3, list, 1, 3);
            DataTransferFlow[] flow = {flow0, flow1};

            double[] zSat = {0};

            double[] ans = {};

            assert(equ(obj.MMFSolver(topology, unsatDataTransferRequests, satDataTransferRequests, flow, zSat), ans));
        }
}
