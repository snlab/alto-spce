#import MFRA_v0
#from MFRA_v0.Requests import Requests
from Requests import Requests
from Scheduler import MFRA

TEST_REQUEST = {
    0: {
   #     'id': 0,
        'source': 0,
        'destination': 1,
        'volume': 100000,
        'lower': 0,
        'upper': 10000000,
    #    'route': [0, 3, 4, 2, 1]
        'route': [0, 1]
    },
    1: {
  #      'id': 1,
        'source': 0,
        'destination': 8,
        'volume': 200000,
        'lower': 1000000,
        'upper': 10000000,
        'route': [0, 6, 4, 3, 8]
    },
    2: {
  #      'id': 2,
        'source': 3,
        'destination': 8,
        'volume': 300000,
        'lower': 2000000,
        'upper': 10000000,
        'route': [3, 8]
    },
    3: {
 #       'id': 3,
        'source': 3,
        'destination': 1,
        'volume': 700000,
        'lower': 3000000,
        'upper': 8000000,
    #    'route': [3, 4, 2, 1]
        'route': [0, 2, 1]
    },
}


unsatRequests = Requests()
unsatRequests.insertReq(TEST_REQUEST[0])
unsatRequests.insertReq(TEST_REQUEST[3])
satRequests = Requests()
satRequests.insertReq(TEST_REQUEST[2])

#print satRequests.reqList

scheduler = MFRA()
scheduler.MMFSolver([3, 2, 1], [0, 200000000, 200000000, 200000000, 0, 200000000, 200000000, 200000000, 0], \
                    unsatRequests.reqList, satRequests.reqList, 0.5)