
from concurrent import futures
import time
import grpc
import service_pb2
import service_pb2_grpc

_ONE_DAY_IN_SECONDS = 60 * 60 * 24

class CarouselServer(service_pb2_grpc.CarouselServiceServicer):

	def getCarousels(self, request, context):
		for i in range(20):
			yield service_pb2.Carousel(id=i, name="Carousel " + str(i), styles = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"] )

	def getCarouselsSync(self, request, context):
		return service_pb2.CarouselResponse(carousels = [service_pb2.Carousel(id=i, name="Carousel " + str(i), styles = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"] ) for i in range(20)])


def serve():
	server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
	service_pb2_grpc.add_CarouselServiceServicer_to_server(CarouselServer(), server)
	server.add_insecure_port('[::]:8080')
	server.start()
	try:
		while True:
			time.sleep(_ONE_DAY_IN_SECONDS)
	except KeyboardInterrupt:
		server.stop(0);

if __name__ == '__main__':
	serve();