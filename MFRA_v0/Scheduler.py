import scipy
print(scipy.__version__)
import numpy
from scipy.optimize import linprog

class MFRA():
    def __init__(self):
        self.requests = {}

    def updateReqs(self, newReqs):
        self.requests = newReqs

    def MMFSolver(self, nodeList, capacityList, mUnsat, mSat, zSat, newAddedMSat, zSatm):
        """
        nodeList: a list of nodeID in increasing order
        capacityList: an adjacent matrix stored in 1D list
        m_unsat: unsaturated requests dictionary
        m_sat: saturated requests dictionary
        z_sat: saturated z value
        decision variables: f_ijm, r_m and z,
        in total: numNodes*numNodes*numFlows+numFlows+1 variables in total
        numNodes*numNodes*numFlows are f_ijm
        numFlows are r_m
        1 is z
        """
        numNodes = nodeList.__len__()
        numFlows = mUnsat.__len__() + mSat.__len__()

        """
        Capacity constraints
        one constraint for each link
        """
        capConstraints = numpy.zeros([numNodes*numNodes, numNodes*numNodes*numFlows+numFlows+1])
        capUB = numpy.zeros([numNodes*numNodes, 1])

        for i in range(nodeList.__len__()):
            for j in range(nodeList.__len__()):
                flowIdx = 0
                for m in mUnsat.keys():
                    for x in range(mUnsat[m]['route'].__len__()-1):
                       # if i==1 and j==2:
                       #     print 1
                        if mUnsat[m]['route'][x] == nodeList[i] and mUnsat[m]['route'][x+1] == nodeList[j]:
                            capConstraints[numNodes*i+j, numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                    flowIdx += 1
                for m in mSat.keys():
                    for x in range(mSat[m]['route'].__len__()-1):
                        if mSat[m]['route'][x] == nodeList[i] and mSat[m]['route'][x+1] == nodeList[j]:
                            capConstraints[numNodes*i+j, numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                    flowIdx += 1
                capUB[numNodes*i+j, 0] = capacityList[numNodes*i+j]

        """
        M_unsat r constraints
        one for each unsat flow
        """
        rUnsatConstraints = numpy.zeros([mUnsat.__len__(), numNodes*numNodes*numFlows+numFlows+1])
        rUnsatUB = numpy.zeros([mUnsat.__len__(), 1])
        flowIdx = 0
        for m in mUnsat.keys():
            rUnsatConstraints[flowIdx, numNodes*numNodes*numFlows+flowIdx] = -1
            rUnsatConstraints[flowIdx, numNodes*numNodes*numFlows+numFlows] = mUnsat[m]['volume']
            flowIdx += 1
        """
        M_sat r constraints, EQ constraints
        one for each sat flow
        """
        rSatConstraints = numpy.zeros([mSat.__len__(), numNodes*numNodes*numFlows+numFlows+1])
        rSatEQUB = numpy.zeros([mSat.__len__(), 1])
        flowIdx = 0
        for m in mSat.keys():
            rSatConstraints[flowIdx, numNodes*numNodes*numFlows+mUnsat.__len__()+flowIdx] = 1
            if m in newAddedMSat.keys():
                rSatEQUB[flowIdx, 0] = mUnsat[m]['volume'] * zSatm
            else:
                rSatEQUB[flowIdx, 0] = mUnsat[m]['volume'] * zSat
            flowIdx += 1

        """
        Route constraints
        numNodes*numNodes*numFlows constraints
        """
        routeConstraints = numpy.zeros([numNodes*numNodes*numFlows, numNodes*numNodes*numFlows+numFlows+1])
        routeEQUB = numpy.zeros([numNodes*numNodes*numFlows, 1])
        flowIdx = 0
        for m in mUnsat.keys():
            for i in range(nodeList.__len__()):
                for j in range(nodeList.__len__()):
                    enRoute = 0
                    for x in range(mUnsat[m]['route'].__len__()-1):
                        if mUnsat[m]['route'][x] == nodeList[i] and mUnsat[m]['route'][x+1] == nodeList[j]:
                            enRoute = 1
                            break
                    if enRoute == 0:
                        routeConstraints[numNodes*numNodes*flowIdx+numNodes*i+j, \
                                         numNodes*numNodes*flowIdx+numNodes*i+j] = 1
            flowIdx += 1
        for m in mSat.keys():
            for i in range(nodeList.__len__()):
                for j in range(nodeList.__len__()):
                    enRoute = 0
                    for x in range(mSat[m]['route'].__len__()-1):
                        if mSat[m]['route'][x] == nodeList[i] and mSat[m]['route'][x+1] == nodeList[j]:
                            enRoute = 1
                            break
                    if enRoute == 0:
                        routeConstraints[numNodes*numNodes*flowIdx+numNodes*i+j, \
                                         numNodes*numNodes*flowIdx+numNodes*i+j] = 1
            flowIdx += 1


        """
        Flow conservation constraints
        numNodes*numFlows constraints
        """
        fConserveConstraints = numpy.zeros([numNodes*numFlows, numNodes*numNodes*numFlows+numFlows+1])
        fConserveEQUB = numpy.zeros([numNodes*numFlows, 1])
        flowIdx = 0
        for m in mUnsat.keys():
            for i in range(nodeList.__len__()):
                if nodeList[i] in mUnsat[m]['route']:
                    if nodeList[i] == mUnsat[m]['route'][0]:
                        for j in range(nodeList.__len__()):
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                    numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                    numNodes*numNodes*flowIdx+numNodes*j+i] = -1
                        fConserveConstraints[numNodes*flowIdx+i, numNodes*numNodes*numFlows+flowIdx] = -1
                    elif nodeList[i] == mUnsat[m]['route'][mUnsat[m]['route'].__len__()-1]:
                        for j in range(nodeList.__len__()):
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                    numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                    numNodes*numNodes*flowIdx+numNodes*j+i] = -1
                        fConserveConstraints[numNodes*flowIdx+i, numNodes*numNodes*numFlows+flowIdx] = 1
                    else:
                        for j in range(nodeList.__len__()):
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                    numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                    numNodes*numNodes*flowIdx+numNodes*j+i] = -1
            flowIdx += 1
        for m in mSat.keys():
            for i in range(nodeList.__len__()):
                if nodeList[i] in mSat[m]['route']:
                    if nodeList[i] == mSat[m]['route'][0]:
                        for j in range(nodeList.__len__()):
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                 numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                 numNodes*numNodes*flowIdx+numNodes*j+i] = -1
                        fConserveConstraints[numNodes*flowIdx+i, numNodes*numNodes*numFlows+flowIdx] = -1
                    elif nodeList[i] == mSat[m]['route'][mSat[m]['route'].__len__()-1]:
                        for j in range(nodeList.__len__()):
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                 numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                 numNodes*numNodes*flowIdx+numNodes*j+i] = -1
                        fConserveConstraints[numNodes*flowIdx+i, numNodes*numNodes*numFlows+flowIdx] = 1
                    else:
                        for j in range(nodeList.__len__()):
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                 numNodes*numNodes*flowIdx+numNodes*i+j] = 1
                            fConserveConstraints[numNodes*flowIdx+i, \
                                                 numNodes*numNodes*flowIdx+numNodes*j+i] = -1
            flowIdx += 1
        """
        priority constraint
        2*numFlows constraints
        """
        priConstraints = numpy.zeros([2*numFlows, numNodes*numNodes*numFlows+numFlows+1])
        priUB = numpy.zeros([2*numFlows, 1])
        flowIdx = 0
        for m in mUnsat.keys():
            priConstraints[flowIdx, numNodes*numNodes*numFlows+flowIdx] = -1 #lower bound
            priUB[flowIdx, 0] = -1*mUnsat[m]['lower']
            priConstraints[numFlows+flowIdx, numNodes*numNodes*numFlows+flowIdx] = 1 #upper bound
            priUB[numFlows+flowIdx, 0] = mUnsat[m]['upper']
            flowIdx += 1
        for m in mSat.keys():
            priConstraints[flowIdx, numNodes*numNodes*numFlows+flowIdx] = -1 #lower bound
            priUB[flowIdx, 0] = -1*mSat[m]['lower']
            priConstraints[numFlows+flowIdx, numNodes*numNodes*numFlows+flowIdx] = 1 #upper bound
            priUB[numFlows+flowIdx, 0] = mSat[m]['upper']
            flowIdx += 1

        """
        z_t<=1 constraint
        """
        ztConstraint = numpy.zeros([1, numNodes*numNodes*numFlows+numFlows+1])
        ztConstraint[0, numNodes*numNodes*numFlows+numFlows] = 1
        ztUB = numpy.ones([1, 1])

        """
        f_iim = 0 constraints
        numNodes*numFlows constraints
        """
        fSelfConstraint = numpy.zeros([numNodes*numFlows, numNodes*numNodes*numFlows+numFlows+1])
        fSelfEQUB = numpy.zeros([numNodes*numFlows, 1])
        for flowIdx in range(numFlows):
            for i in range(nodeList.__len__()):
                fSelfConstraint[numNodes*flowIdx+i,numNodes*numNodes*flowIdx+numNodes*i+i] = 1



        MMF_Aub = numpy.concatenate((capConstraints, rUnsatConstraints, priConstraints, ztConstraint))
        MMF_bub = numpy.concatenate((capUB, rUnsatUB, priUB, ztUB))
        MMF_Aeq = numpy.concatenate((rSatConstraints, routeConstraints, fConserveConstraints, fSelfConstraint))
        MMF_beq = numpy.concatenate((rSatEQUB, routeEQUB, fConserveEQUB, fSelfEQUB))
        #MMF_c = numpy.zeros([1, numNodes*numNodes*numFlows+numFlows+1])
        #MMF_c[0, numNodes*numNodes*numFlows+numFlows] = 1

        print "finish constructing constraints"


        MMF_Aub_list = MMF_Aub.tolist()
        MMF_bub_list = MMF_bub.tolist()
        MMF_Aeq_list = MMF_Aeq.tolist()
        MMF_beq_list = MMF_beq.tolist()
        MMF_c_list = [0]*(numNodes*numNodes*numFlows+numFlows+1)
        MMF_c_list[numNodes*numNodes*numFlows+numFlows] = -1

        res = linprog(MMF_c_list, A_ub=MMF_Aub_list, b_ub=MMF_bub_list, \
                      A_eq=MMF_Aeq_list, b_eq=MMF_beq_list)
        print (res)
        return res

    def testResidual(self, nodeList, capacityList, unsatFlow):
        capRes = 1 #assume there is residual capacity
        #if rm >= 1:
        #    capRes = 0
        #    return
        for x in range(unsatFlow['route'].__len__()-1):
            i = nodeList.index(unsatFlow['route'][x])
            j = nodeList.index(unsatFlow['route'][x+1])
            if capacityList[i*nodeList.__len__()+j] <= 0:
                capRes = 0
                break
        return capRes

    def getOneRoundSchedule(self, nodeList, capacityList, Flows):
        mUnsat = Flows
        mSat = {}
        zSat = 0
        numNodes = nodeList.__len__()
        numFlows = Flows.__len__()
        while mUnsat.__len__() > 0:
            res = self.MMFSolver(nodeList, capacityList, mUnsat, mSat, zSat, {}, 0)
            residualCapacityList = self.updateCapacityList(res.x, capacityList, numNodes, numFlows)

            """
            first move flows whose r_m is 1 to mSat
            """
            flowIdx = 0
            for m in mUnsat.keys():
                if res.x[numNodes*numNodes*numFlows+flowIdx] >= 1.0:
                    mSat[m] = mUnsat[m]
                flowIdx += 1
            for m in mSat.keys():
                del mUnsat[m]

            if mUnsat.__len__() == 0:
                break

            """
            Second perform residual test for each unsat flow and do things
            """
            flowIdx = 0
            for m in mUnsat.keys():
                """if there is no residual capacity for m"""
                if self.testResidual(nodeList, residualCapacityList, mUnsat[m]) == 0:
                    tempMUnsat = mUnsat[m]
                    newAddedMSat = mUnsat.copy()
                    tempMSat = mSat.copy()
                    tempMSat.update(mUnsat)
                    del tempMSat[m]
                    tempRes = self.MMFSolver(nodeList, capacityList, tempMUnsat, tempMSat, zSat, \
                                             newAddedMSat, res.x[res.x.__len__()-1])
                    if tempRes.fun == res.fun or \
                        abs(tempRes.fun - res.fun) <= 0.001:
                        mSat[m] = mUnsat[m]
                        del mUnsat[m]
                        zSat = res.x[res.x.__len__()-1]
            print ('remaining size of mUnsat is ' mUnsat.__len__())




    def updateCapacityList(self, sol, capacityList, numNodes, numFlows):
        """Question: why capacityList changes as newCapacityList changes???"""
        newCapacityList = capacityList.copy()
        for i in range(numNodes):
            for j in range(numNodes):
                for m in range(numFlows):
                    newCapacityList[numNodes*i+j] -= sol[numNodes*numNodes*m+numNodes*i+j]
        return newCapacityList