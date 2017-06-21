package com.rtr.reco.carousel;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;
import com.rtr.reco.carousel.CarouselServiceGrpc.CarouselServiceBlockingStub;
import com.rtr.reco.carousel.CarouselServiceGrpc.CarouselServiceFutureStub;
import com.rtr.reco.carousel.CarouselServiceGrpc.CarouselServiceStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class CarouselClient {
	private final ManagedChannel channel;
	private final CarouselServiceBlockingStub syncStub;
	private final CarouselServiceStub asyncStub;
	private final CarouselServiceFutureStub futureStub;
	
	public CarouselClient(String host, int port) {
		this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
	}
	
	public CarouselClient(ManagedChannelBuilder<?> channelBuilder) {
		channel = channelBuilder.build();
		syncStub = CarouselServiceGrpc.newBlockingStub(channel);
		asyncStub = CarouselServiceGrpc.newStub(channel);
		futureStub = CarouselServiceGrpc.newFutureStub(channel);
	}
	
	public List<Carousel> getCarousels(long userId) {
		CarouselRequest request = CarouselRequest.newBuilder().setUserId(userId).build();
		try {
			List<Carousel> car = Lists.newArrayList();
			Iterator<Carousel> itr = syncStub.getCarousels(request);
			while (itr.hasNext()) {
				car.add(itr.next());
			}
			return car;
		}
		catch (StatusRuntimeException e) {
			//log
			throw e;
		}
	}
	
	public List<Carousel> getCarouselsAsync(long userId) {
		CarouselRequest request = CarouselRequest.newBuilder().setUserId(userId).build();
		try {
			final CountDownLatch finishLatch = new CountDownLatch(1);
			final List<Carousel> car = Lists.newArrayList();
			asyncStub.getCarousels(request, new StreamObserver<Carousel>() {

				@Override
				public void onCompleted() {
					finishLatch.countDown();
				}

				@Override
				public void onError(Throwable e) {
					System.out.println("error: " + e);
					finishLatch.countDown();
				}

				@Override
				public void onNext(Carousel c) {
					car.add(c);
				}
			});
			finishLatch.await(1, TimeUnit.MINUTES);
			return car;
		}
		catch (StatusRuntimeException e) {
			//log
			throw e;
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}
	}
	
	private static abstract class CompletableFuture<T> implements Future<T> {
		abstract boolean complete();
	}
	
	public Future<List<Carousel>> getCarouselsFuture(long userId) {
		CarouselRequest request = CarouselRequest.newBuilder().setUserId(userId).build();
		try {
			final List<Carousel> car = Lists.newArrayList();
			final CompletableFuture<List<Carousel>> future = new CompletableFuture<List<Carousel>>() {
				boolean complete = false;
				boolean canceled = false;
				@Override
				public synchronized boolean cancel(boolean mayInterruptIfRunning) {
					this.canceled = true;
					this.notify();
					return canceled;
				}

				@Override
				public synchronized List<Carousel> get() throws InterruptedException, ExecutionException {
					if (!complete) {
						this.wait();
					}
					if (this.canceled) {
						throw new InterruptedException("Future is canceled");
					}
					return car;
				}

				@Override
				public synchronized List<Carousel> get(long arg0, TimeUnit arg1)
						throws InterruptedException, ExecutionException, TimeoutException {
					return get();
				}

				@Override
				public synchronized boolean isCancelled() {
					return canceled;
				}

				@Override
				public synchronized boolean isDone() {
					return complete;
				}
				
				@Override
				synchronized boolean complete() {
					this.complete = true;
					this.notify();
					return this.complete;
				}
			};
			asyncStub.getCarousels(request, new StreamObserver<Carousel>() {

				@Override
				public void onCompleted() {
					future.complete();
				}

				@Override
				public void onError(Throwable e) {
					System.out.println("error: " + e);
					future.cancel(true);
				}

				@Override
				public void onNext(Carousel c) {
					car.add(c);
				}
			});

			return future;
		}
		catch (StatusRuntimeException e) {
			//log
			throw e;
		}
	}
	
	public List<Carousel> getCarouselsSync(long userId) {
		CarouselRequest request = CarouselRequest.newBuilder().setUserId(userId).build();
		try {
			CarouselResponse response = syncStub.getCarouselsSync(request);
			return response.getCarouselsList();
		}
		catch (StatusRuntimeException e) {
			//log
			throw e;
		}
	}
	
	public static void main(String... strings) throws Exception {
		CarouselClient client = new CarouselClient("localhost", 8080);
		List<Carousel> carousels= Lists.newArrayList();
		long start = System.currentTimeMillis();
		for (int i=0; i < 1000; i++) {
			carousels = client.getCarouselsSync(i);
		}
		long end = System.currentTimeMillis();
		/*System.out.println("Time: " + (end - start) + " ms");
		
		List<Future<List<Carousel>>> futures = Lists.newArrayList();
		start = System.currentTimeMillis();
		for (int i=0; i < 1000; i++) {
			futures.add(client.getCarouselsFuture(i));
		}
		for (Future<List<Carousel>> f: futures) {
			f.get();
		}
		end = System.currentTimeMillis();*/
		System.out.println("Time: " + (end - start) + " ms");
		System.out.println("Time: " + (double)(end - start)/1000.0 + " average request time (ms)");
		System.out.println("Time: " + 1000.0/((double)(end - start)/1000.0)+ " requests/second");
		/*for (Carousel c : carousels) {
			System.out.println("{ id=" + c.getId() + ", name=" + c.getName() + ", styles=" + Arrays.toString(c.getStylesList().toArray()) + " }");
		}*/
	}
}
