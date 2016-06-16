#!/usr/bin/env python

import pulp
import time
from numpy import array
from odl.instance import ODLInstance
from odl.altospce import ALTOSpce

# Default time slot is 5 min
TIME_SLOT = 300

# Default options
DEFAULT_OPTS = {
    'controller': 'localhost:8181',
    'login': 'admin',
    'password': 'admin'
}

# Test data
TEST_REQUEST = {
    0: {
        'id': 0,
        'source': 0,
        'destination': 1,
        'volume': 100000,
        'lower': 0,
        'upper': 10000000,
        'route': [0, 3, 4, 2, 1]
    },
    1: {
        'id': 1,
        'source': 0,
        'destination': 8,
        'volume': 200000,
        'lower': 1000000,
        'upper': 10000000,
        'route': [0, 6, 4, 3, 8]
    },
    2: {
        'id': 2,
        'source': 3,
        'destination': 8,
        'volume': 300000,
        'lower': 2000000,
        'upper': 10000000,
        'route': [3, 8]
    },
    3: {
        'id': 3,
        'source': 3,
        'destination': 1,
        'volume': 700000,
        'lower': 3000000,
        'upper': 8000000,
        'route': [3, 4, 2, 1]
    },
}

TEST_TX_PORT_BANDWIDTH = [
    10000000,
    10000000,
    10000000,
    40000000,
    40000000,
    10000000,
    100000000,
    100000000,
    40000000
]

class Scheduler():
    def __init__(self, options=DEFAULT_OPTS):
        self.options = options
        self.requests = {}
        self.test_mode = options.get('test', False)

    def setup_controller(self):
        self.odl = ODLInstance(
            self.options['controller'],
            (self.options['login'], self.options['password']))
        self.spce = ALTOSpce(self.odl)

    def get_capacity(self):
        if self.test_mode:
            return TEST_TX_PORT_BANDWIDTH
        else:
            return array(self.spce.get_tx_port_bandwidth())

    def get_requests(self):
        if self.test_mode:
            return TEST_REQUEST
        else:
            return {}

    def test_residual_capacity(self, capacity, alloc, requests, r_id):
        """
        Test the residual capacity in the given route
        """
        for i in requests[r_id]['route']:
            alloc_bandwidth = sum([alloc[m_id]
                                   for m_id in requests.keys()
                                   if i in requests[m_id]['route']])
            bandwidth_upbound = min(capacity[i] - alloc_bandwidth,
                                    requests[r_id]['upper'])
            residual_capacity = bandwidth_upbound - alloc[r_id]
            if residual_capacity <= 0:
                return False
        return True

    def mmf_solver(self, capacity, m_unsat, m_sat, z_sat):
        """
        Solve MMF problem.

        capacity: tx port capacity
        m_unsat: unsaturated requests list
        m_sat: saturated requests list
        z_sat: saturated z value
        """
        # Display input
        print "======== INPUT ========"
        print "capacity=", capacity
        print "m_unsat=", m_unsat
        print "m_sat=", m_sat
        print "z_sat=", z_sat

        assert set(m_sat.keys()) <= set(z_sat.keys())

        # Problem definition
        solver = pulp.LpProblem('mmf', pulp.LpMaximize)

        port_num = len(capacity)
        flows = m_unsat.copy()
        flows.update(m_sat)
        # flow_num = len(flows)

        # Statements and natural constraints
        flow_alloc = {m['id']: pulp.LpVariable('flow_'+str(m['id']),
                                               m['lower'], m['upper'])
                      for m in flows.values()}
        z = pulp.LpVariable('z', 0)

        # Capacity constraints
        for i in range(port_num):
            solver += sum([flow_alloc[m_id]
                           for m_id in flows.keys()
                           if i in flows[m_id]['route']]) <= capacity[i]

        # Saturation constraints
        for m_id in m_unsat.keys():
            solver += flow_alloc[m_id] >= z * m_unsat[m_id]['volume']
        for m_id in m_sat.keys():
            solver += flow_alloc[m_id] == z_sat[m_id] * m_sat[m_id]['volume']

        # Objective
        solver += z

        # Solve problem
        solver.solve()
        alloc = {m_id: flow_alloc[m_id].varValue
                 for m_id in flows.keys()}
        optimal_z = z.varValue

        # Display output
        print "======== OUTPUT ========"
        print "Allocation=", alloc
        print "Optimal Z=", optimal_z

        return alloc, optimal_z

    def do_schedule(self, capacity, requests):
        """
        Execute one step MFRA schedule
        """
        m_sat = {}
        m_unsat = requests.copy()
        z_sat = {}
        while m_unsat:
            # Solve MMF problem
            alloc, z = self.mmf_solver(capacity, m_unsat, m_sat, z_sat)

            # Set exit flag
            exit_flag = True
            for m in m_unsat.values():
                # Saturation test
                if self.test_residual_capacity(capacity, alloc, requests, m['id']):
                    continue

                # Modify exit flag
                exit_flag = False

                tmp_m_unsat = {m['id']: m}
                tmp_m_sat = requests.copy()
                tmp_m_sat.pop(m['id'])
                tmp_z_sat = z_sat.copy()
                for tmp_m in tmp_m_sat.values():
                    if tmp_m['id'] not in tmp_z_sat.keys():
                        tmp_z_sat[tmp_m['id']] = z
                tmp_alloc, tmp_z = self.mmf_solver(
                    capacity, tmp_m_unsat, tmp_m_sat, tmp_z_sat)
                if tmp_z == z:
                    # Update m_sat and m_unsat
                    m_sat[m['id']] = m
                    m_unsat.pop(m['id'])
                    z_sat[m['id']] = z

            # Exit if flag not changed
            if exit_flag:
                break

        # Deliver rate allocation
        return alloc

    def auto_schedule(self, time_slot=TIME_SLOT):
        """
        Automatic reschedule by interval time slot
        """
        while True:
            # Update capacity and requests
            capacity = self.get_capacity()
            requests = self.get_requests()

            if len(requests):
                # Do schedule
                policy = self.do_schedule(capacity, requests)
                self.update_policy(policy)

            # Sleep and wait for next reschedule
            time.sleep(time_slot)
        return

    def update_policy(self, policy, requests):
        """
        Update rate limit policy for each request
        """
        for m_id in requests.keys():
            src = requests[m_id]['source']
            dst = requests[m_id]['destination']
            alloc = policy[m_id]
            self.apply_rate_limit(src, dst, round(alloc))
        return

    def apply_rate_limit(self, src, dst, alloc):
        """
        Apply a rate limit policy by OpenDaylight
        """
        print "Apply rate policy for src=%s, dst=%s, rate=%d" % (src, dst, alloc)
        if not self.test_mode:
            self.spce.update_tc(src=src, dst=dst, bd=alloc, bs=alloc)

    def test(self):
        mode = self.test_mode
        self.test_mode = True
        capacity = self.get_capacity()
        requests = self.get_requests()
        policy = self.do_schedule(capacity, requests)
        self.update_policy(policy, requests)
        self.test_mode = mode

if '__main__' == __name__:
    scheduler = Scheduler()
    scheduler.test()
