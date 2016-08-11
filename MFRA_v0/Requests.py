#request format: ID (key), src, dst, volume,
#               lower bound, upper bound, route
#               status,




class Requests():
    reqList = {}

    def __init__(self):
        self.reqList = {}

    def insertReq(self, newReq):
        print self.reqList.keys()
        if self.reqList.__len__() == 0:
            self.reqList[0] = newReq
        else:
            self.reqList[max(self.reqList.keys())+1] = newReq

    def deleteReq(self, reqID):
        if self.reqList.get(reqID):
            self.reqList.__delitem__(reqID)
            print 'Request', reqID, 'has been deleted for good'
        else:
            print 'This request does not exist!'

    def getAllReq(self):
        return self.reqList

    #   def getActiveReq():
    #   def updateReq(self, reqID):

#requests = Requests()
#requests.insertReq(TEST_REQUEST[0])
#requests.insertReq(TEST_REQUEST[3])
#requests.deleteReq(2)
#requests.deleteReq(0)
#requests.insertReq(TEST_REQUEST[0])

#print requests.reqList


