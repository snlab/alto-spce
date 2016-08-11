import scipy
print(scipy.__version__)
import numpy
from scipy.optimize import linprog

class MFRA():
    def __init__(self):
        self.requests = {}

    def updateReqs(self, newReqs):
        self.requests = newReqs

    def MMFSolver(self, nodeList, capacityList, mUnsat, mSat, zSat):
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
            rSatConstraints[flowIdx, numNodes*numNodes*numFlows+flowIdx] = 1
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


        """
        Flow conservation constraints
        numNodes*numFlows consstraints
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
        for m in mSat.keys():
            priConstraints[flowIdx, numNodes*numNodes*numFlows+flowIdx] = -1 #lower bound
            priUB[flowIdx, 0] = -1*mSat[m]['lower']
            priConstraints[numFlows+flowIdx, numNodes*numNodes*numFlows+flowIdx] = 1 #upper bound
            priUB[numFlows+flowIdx, 0] = mSat[m]['upper']
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

        MMF_Aub = numpy.concatenate((capConstraints, rUnsatConstraints, priConstraints, ztConstraint))
        MMF_bub = numpy.concatenate((capUB, rUnsatUB, priUB, ztUB))
        MMF_Aeq = numpy.concatenate((rSatConstraints, routeConstraints, fConserveConstraints))
        MMF_beq = numpy.concatenate((rSatEQUB, routeEQUB, fConserveEQUB))
        #MMF_c = numpy.zeros([1, numNodes*numNodes*numFlows+numFlows+1])
        #MMF_c[0, numNodes*numNodes*numFlows+numFlows] = 1

        print "finish constructing constraints"


        MMF_Aub_list = MMF_Aub.tolist()
        MMF_bub_list = MMF_bub.tolist()
        MMF_Aeq_list = MMF_Aeq.tolist()
        MMF_beq_list = MMF_beq.tolist()
        MMF_c_list = [0]*(numNodes*numNodes*numFlows+numFlows+1)
        MMF_c_list[numNodes*numNodes*numFlows+numFlows] = 1

        res = linprog(MMF_c_list, A_ub=MMF_Aub_list, b_ub=MMF_bub_list, \
                      A_eq=MMF_Aeq_list, b_eq=MMF_beq_list)
        print (res)

