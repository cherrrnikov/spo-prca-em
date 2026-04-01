<script lang="ts">
  import Modal from '$lib/components/Modal.svelte';
  import { modal } from '$lib/services/modal.service';
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

<Modal
    isOpen={$modal.isOpen}
    title={$modal.title}
    message={$modal.message}
    type={$modal.type}
    showConfirm={$modal.showConfirm}
    confirmText={$modal.confirmText}
    cancelText={$modal.cancelText}
    onConfirm={() => {
        $modal.onConfirm?.();
        modal.close();
    }}
    onCancel={() => {
        $modal.onCancel?.();
        modal.close();
    }}
    onClose={modal.close}
/>

{@render children()}

