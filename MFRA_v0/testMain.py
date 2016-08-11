#import MFRA_v0
#from MFRA_v0.Requests import Requests
from Requests import Requests
from Scheduler import MFRA

TEST_REQUEST = {
    0: {
   #     'id': 0,
        'source': 2,
        'destination': 1,
        'volume': 1,
        'lower': 0,
        'upper': 10000000,
    #    'route': [0, 3, 4, 2, 1]
        'route': [2, 1]
    },
    1: {
  #      'id': 1,
        'source': 1,
        'destination': 3,
        'volume': 1,
        'lower': 0,
        'upper': 10000000,
        'route': [1, 2, 3]
    },
    2: {
  #      'id': 2,
        'source': 1,
        'destination': 3,
        'volume': 1,
        'lower': 0,
        'upper': 10000000,
        'route': [1, 3]
    },
    3: {
 #       'id': 3,
        'source': 3,
        'destination': 1,
        'volume': 1,
        'lower': 0,
        'upper': 8000000,
    #    'route': [3, 4, 2, 1]
        'route': [3, 2, 1]
    },
}


unsatRequests = Requests()
unsatRequests.insertReq(TEST_REQUEST[0])
unsatRequests.insertReq(TEST_REQUEST[3])
satRequests = Requests()
satRequests.insertReq(TEST_REQUEST[2])

#print satRequests.reqList

scheduler = MFRA()

links = [3, 2, 1]
capacity = [0, 5, 7, \
            6, 0, 4, \
            6, 9, 0]

scheduler.MMFSolver(links, capacity, \
                    unsatRequests.reqList, satRequests.reqList, 0)