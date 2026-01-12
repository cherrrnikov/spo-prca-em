<script lang="ts">
  import { onDestroy, onMount } from 'svelte';
  import '../app.css';
  
  let keepaliveInterval: NodeJS.Timeout;

  let { children } = $props();
  
  onMount(() => {
    keepaliveInterval = setInterval(async () => {
      try {
        await fetch('/api/auth/keepalive', {
          method: 'GET',
          credentials: 'same-origin' 
        });
      } catch (error) {
      }
    }, 60000);
    
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        fetch('/api/auth/keepalive', {
          method: 'GET',
          credentials: 'same-origin'
        }).catch(() => {});
      }
    });
  });
  
  onDestroy(() => {
    if (keepaliveInterval) clearInterval(keepaliveInterval);
  });
</script>

<svelte:head>
	<!-- <link rel="icon" href={favicon} /> -->
</svelte:head>

{@render children()}
