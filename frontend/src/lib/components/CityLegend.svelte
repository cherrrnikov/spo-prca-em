<script lang="ts">
  import type { City } from '$lib/types/schedule';
  
  export let cities: City[];
</script>

<div class="city-legend">
  <div class="cities-list">
    {#each cities as city}
      <div class="city-item">
        <div 
          class="color-square" 
          style="background: {city.color}"
          title={city.name}
        ></div>
        <span class="city-name">{city.name}</span>
      </div>
    {/each}
  </div>
</div>

<style>
  .city-legend {
    display: flex;
    align-items: center;
    gap: 1rem;
    max-width: 30rem;
  }

  .cities-list {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    flex-wrap: wrap;
  }

  .city-item {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .color-square {
    width: 16px;
    height: 16px;
    border-radius: 3px;
    border: 2px solid white;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  }

  .city-name {
    font-size: 0.9rem;
    color: #2d3748;
  }
</style>