package main

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

// Task represents a unit of work
type Task struct {
	ID int
}

// process simulates computation
func (t Task) process() error {
	fmt.Printf("%s is processing Task %d\n", getGoroutineName(), t.ID)
	time.Sleep(time.Millisecond * time.Duration(300+rand.Intn(400))) // simulate delay
	return nil
}

// getGoroutineName returns a pseudo-name for logging (since Go doesn't expose goroutine ID)
func getGoroutineName() string {
	return fmt.Sprintf("Worker-%d", rand.Intn(1000)) // pseudo-ID
}

// worker function pulls from task channel and processes tasks
func worker(id int, tasks <-chan Task, results *[]string, mu *sync.Mutex, wg *sync.WaitGroup) {
	defer wg.Done()
	name := fmt.Sprintf("Worker-%d", id)

	for task := range tasks {
		err := task.process()
		if err != nil {
			fmt.Printf("%s encountered an error: %v\n", name, err)
			continue
		}
		// Save result with locking
		mu.Lock()
		*results = append(*results, fmt.Sprintf("%s processed Task %d", name, task.ID))
		mu.Unlock()
	}
	fmt.Println(name + " finished processing.")
}

func main() {
	rand.Seed(time.Now().UnixNano())

	// Create shared task channel and results list
	taskChan := make(chan Task, 20)
	var results []string
	var mu sync.Mutex
	var wg sync.WaitGroup

	// Fill the task channel
	for i := 1; i <= 20; i++ {
		taskChan <- Task{ID: i}
	}
	close(taskChan) // No more tasks will be sent

	// Launch worker goroutines
	numWorkers := 4
	for i := 1; i <= numWorkers; i++ {
		wg.Add(1)
		go worker(i, taskChan, &results, &mu, &wg)
	}

	// Wait for all workers to finish
	wg.Wait()

	// Print results
	fmt.Println("\nFinal Results:")
	for _, res := range results {
		fmt.Println(res)
	}
}
